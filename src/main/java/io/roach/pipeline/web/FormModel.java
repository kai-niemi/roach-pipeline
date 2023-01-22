package io.roach.pipeline.web;

import org.springframework.hateoas.RepresentationModel;

public abstract class FormModel<T extends FormModel<? extends T>> extends RepresentationModel<T> {
    private String table;

    private long restartExecutionId;

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public long getRestartExecutionId() {
        return restartExecutionId;
    }

    public void setRestartExecutionId(long restartExecutionId) {
        this.restartExecutionId = restartExecutionId;
    }
}
