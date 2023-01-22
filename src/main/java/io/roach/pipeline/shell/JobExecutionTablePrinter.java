package io.roach.pipeline.shell;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.converter.JsonJobParametersConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.stereotype.Component;

@Component
public class JobExecutionTablePrinter {
    private static final String HEADER = "%10s | %8s | %10s | %20s | %20s | %20s | %s\n";

    @Autowired
    private Console console;

    public void printTable(List<JobExecution> jobExecutions) {
        printJobExecutionHeader();
        for (JobExecution jobExecution : jobExecutions) {
            printJobExecution(jobExecution);
        }
    }

    private void printJobExecutionHeader() {
        console.printf(AnsiColor.BRIGHT_YELLOW,
                HEADER,
                "Name",
                "ID",
                "Status",
                "Start Time",
                "End Time",
                "Duration",
                "Exit Status"
        );
    }

    private AnsiColor colorForStatus(BatchStatus batchStatus) {
        return switch (batchStatus) {
            case FAILED -> AnsiColor.RED;
            case STARTING, STARTED -> AnsiColor.BRIGHT_GREEN;
            case STOPPED -> AnsiColor.MAGENTA;
            case UNKNOWN -> AnsiColor.WHITE;
            case STOPPING -> AnsiColor.BLUE;
            case ABANDONED -> AnsiColor.CYAN;
            case COMPLETED -> AnsiColor.BRIGHT_WHITE;
        };
    }

    public void printJobExecution(JobExecution jobExecution) {
        console.printf(
                colorForStatus(jobExecution.getStatus()), HEADER,
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId(),
                jobExecution.getStatus(),
                formatDateTime(jobExecution.getStartTime()),
                formatDateTime(jobExecution.getEndTime()),
                executionDuration(jobExecution.getStartTime(), jobExecution.getEndTime()),
                jobExecution.getExitStatus().getExitCode()
        );
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
    }

    public void printJobExecutionDetail(JobExecution jobExecution) {
        printMetric("ID", jobExecution.getId());
        printMetric("Name", jobExecution.getJobInstance().getJobName());
        printMetric("Status", jobExecution.getStatus());
        printMetric("Start time", jobExecution.getStartTime());
        printMetric("End time", jobExecution.getEndTime() != null ? jobExecution.getEndTime() : "N/A");
        printMetric("Duration", executionDuration(jobExecution.getStartTime(), jobExecution.getEndTime()));
        printMetric("Last Updated", jobExecution.getLastUpdated());
        printMetric("Exit Code", jobExecution.getExitStatus().getExitCode());
        printMetric("Exit Description", jobExecution.getExitStatus().getExitDescription());

        JsonJobParametersConverter parametersConverter = new JsonJobParametersConverter();
        Properties properties = parametersConverter.getProperties(jobExecution.getJobParameters());

        printMetric("Parameters", "(" + properties.size() + ")");
        properties.forEach((k, v) -> {
            printMetric((String) k, v, 24, AnsiColor.BRIGHT_WHITE);
        });

        printMetric("Step Executions", "(" + jobExecution.getStepExecutions().size() + ")");

        jobExecution.getStepExecutions().forEach(stepExecution -> {
            Duration duration = executionDuration(stepExecution.getStartTime(), stepExecution.getEndTime());
            double readsPerSec = stepExecution.getReadCount() * 1f / Math.max(1, duration.toMillis() / 1000.0);
            double writesPerSec = stepExecution.getWriteCount() * 1f / Math.max(1, duration.toMillis() / 1000.0);

            printMetric("Step Execution ID", stepExecution.getJobExecutionId(), 24,AnsiColor.BRIGHT_WHITE);
            printMetric("Start time", stepExecution.getStartTime(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("End time", stepExecution.getEndTime() != null ? stepExecution.getEndTime() : "N/A", 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Duration", duration, 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Read count", stepExecution.getReadCount(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Read skip count", stepExecution.getReadSkipCount(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Reads per sec", readsPerSec, 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Write count", stepExecution.getWriteCount(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Write skip count", stepExecution.getWriteSkipCount(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Writes per sec", readsPerSec, 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Total r/w per sec", readsPerSec + writesPerSec, 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Filter count", stepExecution.getFilterCount(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Process skip count", stepExecution.getProcessSkipCount(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Commit count", stepExecution.getCommitCount(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Rollback count", stepExecution.getRollbackCount(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Exit code", stepExecution.getExitStatus().getExitCode(), 24, AnsiColor.BRIGHT_WHITE);
            printMetric("Exit description", stepExecution.getExitStatus().getExitDescription(), 24, AnsiColor.BRIGHT_WHITE);
        });

        printMetric("Failure Exceptions", "(" + jobExecution.getAllFailureExceptions().size() + ")");
        jobExecution.getAllFailureExceptions().forEach(t -> {
            printMetric("", t.toString(), 24, AnsiColor.BRIGHT_RED);
        });
    }

    private Duration executionDuration(LocalDateTime startTime, LocalDateTime endTime) {
        return startTime != null && endTime != null
                ? Duration.between(startTime, endTime)
                : startTime != null
                ? Duration.between(startTime.toInstant(ZoneOffset.UTC), Instant.now())
                : Duration.ZERO;
    }


    private void printMetric(String label, Object value) {
        printMetric(label, value, 20, AnsiColor.BRIGHT_YELLOW);
    }

    private void printMetric(String label, Object value, int indent) {
        printMetric(label, value, indent, AnsiColor.BRIGHT_YELLOW);
    }

    private void printMetric(String label, Object value, int indent, AnsiColor labelColor) {
        console.printf(labelColor, "%" + indent + "s: ", label);
        if (value instanceof Float || value instanceof Double) {
            console.printf(AnsiColor.BRIGHT_CYAN, "%.2f\n", value);
        } else {
            console.printf(AnsiColor.BRIGHT_CYAN, "%s\n", value);
        }
    }
}

