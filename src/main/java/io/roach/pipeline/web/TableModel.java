package io.roach.pipeline.web;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"links"})
public class TableModel extends RepresentationModel<TableModel> {
    public static TableModel of(String message) {
        return new TableModel(message);
    }

    private String table;

    public TableModel(String table) {
        this.table = table;
    }

    public String getTable() {
        return table;
    }
}
