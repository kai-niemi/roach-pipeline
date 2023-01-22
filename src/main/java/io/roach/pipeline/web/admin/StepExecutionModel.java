package io.roach.pipeline.web.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.web.LinkRels;

@Relation(itemRelation = LinkRels.STEP_EXECUTION_REL, collectionRelation = LinkRels.STEP_EXECUTIONS_REL)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"links", "embedded"})
public class StepExecutionModel extends RepresentationModel<StepExecutionModel> {
    private String stepName;

    private BatchStatus status;

    private long readCount;

    private long writeCount;

    private long commitCount;

    private long rollbackCount;

    private long readSkipCount;

    private long processSkipCount;

    private long writeSkipCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime lastUpdated;

    private ExitStatus exitStatus;

    private boolean terminateOnly;

    private long filterCount;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Throwable> failureExceptions;

    public StepExecutionModel setStepName(String stepName) {
        this.stepName = stepName;
        return this;
    }

    public StepExecutionModel setStatus(BatchStatus status) {
        this.status = status;
        return this;
    }

    public StepExecutionModel setReadCount(long readCount) {
        this.readCount = readCount;
        return this;
    }

    public StepExecutionModel setWriteCount(long writeCount) {
        this.writeCount = writeCount;
        return this;
    }

    public StepExecutionModel setCommitCount(long commitCount) {
        this.commitCount = commitCount;
        return this;
    }

    public StepExecutionModel setRollbackCount(long rollbackCount) {
        this.rollbackCount = rollbackCount;
        return this;
    }

    public StepExecutionModel setReadSkipCount(long readSkipCount) {
        this.readSkipCount = readSkipCount;
        return this;
    }

    public StepExecutionModel setProcessSkipCount(long processSkipCount) {
        this.processSkipCount = processSkipCount;
        return this;
    }

    public StepExecutionModel setWriteSkipCount(long writeSkipCount) {
        this.writeSkipCount = writeSkipCount;
        return this;
    }

    public StepExecutionModel setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public StepExecutionModel setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public StepExecutionModel setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public StepExecutionModel setExitStatus(ExitStatus exitStatus) {
        this.exitStatus = exitStatus;
        return this;
    }

    public StepExecutionModel setTerminateOnly(boolean terminateOnly) {
        this.terminateOnly = terminateOnly;
        return this;
    }

    public StepExecutionModel setFilterCount(long filterCount) {
        this.filterCount = filterCount;
        return this;
    }

    public StepExecutionModel setFailureExceptions(List<Throwable> failureExceptions) {
        this.failureExceptions = failureExceptions;
        return this;
    }

    public String getStepName() {
        return stepName;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public long getReadCount() {
        return readCount;
    }

    public long getWriteCount() {
        return writeCount;
    }

    public long getCommitCount() {
        return commitCount;
    }

    public long getRollbackCount() {
        return rollbackCount;
    }

    public long getReadSkipCount() {
        return readSkipCount;
    }

    public long getProcessSkipCount() {
        return processSkipCount;
    }

    public long getWriteSkipCount() {
        return writeSkipCount;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public ExitStatus getExitStatus() {
        return exitStatus;
    }

    public boolean isTerminateOnly() {
        return terminateOnly;
    }

    public long getFilterCount() {
        return filterCount;
    }

    public List<Throwable> getFailureExceptions() {
        return failureExceptions;
    }
}
