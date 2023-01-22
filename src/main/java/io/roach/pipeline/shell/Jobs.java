package io.roach.pipeline.shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

import io.roach.pipeline.item.BatchJobRepository;

@ShellComponent
@ShellCommandGroup(CommandGroups.JOBS)
public class Jobs {
    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private BatchJobRepository jobBatchRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionTablePrinter jobExecutionTablePrinter;

    @Autowired
    private Console console;

    @ShellMethod(value = "List job names")
    public void names() {
        console.printf(AnsiColor.BRIGHT_CYAN, "%10s | %8s\n", "Name", "Instance Count");
        jobExplorer.getJobNames().forEach(jobName -> {
            try {
                long instanceCount = jobExplorer.getJobInstanceCount(jobName);
                console.printf(AnsiColor.MAGENTA, "%10s: ", jobName);
                console.printf(AnsiColor.BRIGHT_CYAN, "%s\n", instanceCount);
            } catch (NoSuchJobException e) {
                e.printStackTrace();
            }
        });
    }

    @ShellMethod(value = "List job executions")
    public void jobs(@ShellOption(help = "filter on job status", defaultValue = "") String status,
                     @ShellOption(help = "filter on job name", defaultValue = "") String name,
                     @ShellOption(help = "filter on last instance", defaultValue = "false") boolean last,
                     @ShellOption(help = "filter on running jobs", defaultValue = "false") boolean running,
                     @ShellOption(help = "max jobs to list", defaultValue = "0") int limit) {
        List<JobExecution> jobExecutions = new ArrayList<>();

        if (StringUtils.hasLength(name)) {
            if (last) {
                Optional.ofNullable(jobExplorer.getLastJobInstance(name)).ifPresent(
                        jobInstance -> jobExecutions.addAll(jobExplorer.getJobExecutions(jobInstance)));
            } else {
                jobExplorer.getJobInstances(name, 0, limit > 0 ? limit : Integer.MAX_VALUE).forEach(
                        jobInstance -> jobExecutions.addAll(jobExplorer.getJobExecutions(jobInstance)));
            }
        } else {
            jobExplorer.getJobNames().forEach(jobName -> {
                if (running) {
                    jobExecutions.addAll(jobExplorer.findRunningJobExecutions(jobName));
                } else {
                    jobExplorer.getJobInstances(jobName, 0, limit > 0 ? limit : Integer.MAX_VALUE).forEach(
                            jobInstance -> jobExecutions.addAll(jobExplorer.getJobExecutions(jobInstance)));
                }
            });
        }

        jobExecutions.sort((e1, e2) -> Long.compareUnsigned(e1.getJobId(), e2.getJobId()));

        Set<BatchStatus> statusFilter = StringUtils.hasLength(status)
                ? Arrays.stream(status.split(",")).map(BatchStatus::valueOf).collect(Collectors.toSet())
                : Collections.emptySet();

        jobExecutionTablePrinter.printTable(jobExecutions.stream()
                .filter(jobExecution -> statusFilter.isEmpty() || statusFilter.contains(jobExecution.getStatus()))
                .collect(Collectors.toList()));
    }

    @ShellMethod(value = "Purge all job metadata")
    public void purge() {
        console.information("Delete all jobs");
        int rows = jobBatchRepository.purgeAll();
        console.printf(AnsiColor.BRIGHT_CYAN, "%d rows deleted\n", rows);
    }

    @ShellMethod(value = "Delete all job details")
    public void delete(@ShellOption(help = "job name") String name) {
        jobExplorer.getJobInstances(name, 0, Integer.MAX_VALUE).forEach(jobInstance -> {
            jobExplorer.getJobExecutions(jobInstance).forEach(jobExecution -> {
                jobExecution.getStepExecutions().forEach(stepExecution -> {
                    console.information("Delete step execution: " + stepExecution);
                    jobRepository.deleteStepExecution(stepExecution);
                });
                console.information("Delete job execution: " + jobExecution);
                jobRepository.deleteJobExecution(jobExecution);
            });
            console.information("Delete job instance: " + jobInstance);
            jobRepository.deleteJobInstance(jobInstance);
        });
    }

    @ShellMethod(value = "Delete job execution")
    public void delete(@ShellOption(help = "job execution id") long executionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        if (jobExecution != null) {
            jobExecution.getStepExecutions().forEach(stepExecution -> {
                console.information("Delete step execution: " + stepExecution);
                jobRepository.deleteStepExecution(stepExecution);
            });
            console.information("Delete job execution: " + jobExecution);
            jobRepository.deleteJobExecution(jobExecution);
        } else {
            console.warning("No job execution with id: " + executionId);
        }
    }

    @ShellMethod(value = "List job execution details")
    public void job(@ShellOption(help = "job execution id") long executionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        if (jobExecution != null) {
            jobExecutionTablePrinter.printJobExecutionDetail(Objects.requireNonNull(jobExecution));
        } else {
            console.warning("No job execution with id: " + executionId);
        }
    }

    @ShellMethod(value = "Stop job execution")
    public void stop(@ShellOption(help = "job execution id") long executionId) {
        try {
            boolean sent = jobOperator.stop(executionId);
            if (sent) {
                console.information("Stop request sent successfully");
            } else {
                console.warning("Stop request rejected");
            }
        } catch (NoSuchJobExecutionException | JobExecutionNotRunningException e) {
            console.warning(e.getMessage());
        }
    }

    @ShellMethod(value = "Restart job execution", key = {"restart", "resume"})
    public void restart(@ShellOption(help = "job execution id") long executionId) {
        try {
            console.printf(AnsiColor.YELLOW, "Restart job with id: %d\n", executionId);
            executionId = jobOperator.restart(executionId);
            console.printf(AnsiColor.GREEN, "Job restart with new id: %d\n", executionId);
        } catch (NoSuchJobException e) {
            console.warning(e.getMessage());
            console.warning(
                    "If the job ID is listed you need to re-register the job and provide the ID in the 'restartExecutionId' parameter");
        } catch (JobInstanceAlreadyCompleteException | NoSuchJobExecutionException
                 | JobRestartException | JobParametersInvalidException e) {
            console.warning(e.getMessage());
        }
    }

    @ShellMethod(value = "Abandon job execution")
    public void abandon(@ShellOption(help = "job execution id") long executionId) {
        try {
            JobExecution execution = jobOperator.abandon(executionId);
            jobExecutionTablePrinter.printJobExecution(execution);
        } catch (NoSuchJobExecutionException | JobExecutionAlreadyRunningException e) {
            console.warning(e.toString());
        }
    }
}
