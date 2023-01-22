package io.roach.pipeline.web.admin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.util.TimeUtils;
import io.roach.pipeline.web.LinkRels;

@Relation(value = LinkRels.JOB_EXECUTION_REL,
        collectionRelation = LinkRels.JOB_EXECUTIONS_REL)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"links", "embedded"})
public class JobExecutionModel extends RepresentationModel<JobExecutionModel> {
    private String jobName;

    private JobParameters jobParameters;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BatchStatus batchStatus = BatchStatus.UNKNOWN;

    private ExitStatus exitStatus = ExitStatus.NOOP;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Throwable> failures = Collections.emptyList();

    public JobParameters getJobParameters() {
        return jobParameters;
    }

    public JobExecutionModel setJobParameters(JobParameters jobParameters) {
        this.jobParameters = jobParameters;
        return this;
    }

    public JobExecutionModel setJobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    public JobExecutionModel setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public JobExecutionModel setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public JobExecutionModel setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
        return this;
    }

    public JobExecutionModel setExitStatus(ExitStatus exitStatus) {
        this.exitStatus = exitStatus;
        return this;
    }

    public JobExecutionModel setFailures(List<Throwable> failures) {
        this.failures = failures;
        return this;
    }

    public String getJobName() {
        return jobName;
    }

    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public ExitStatus getExitStatus() {
        return exitStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public long getExecutionTimeMillis() {
        return startTime != null && endTime != null ?
                Duration.between(startTime, endTime).toMillis() : 0;
    }

    public String getExecutionTime() {
        return TimeUtils.millisecondsToDisplayString(getExecutionTimeMillis());
    }

    public List<Throwable> getFailures() {
        return failures;
    }
}
