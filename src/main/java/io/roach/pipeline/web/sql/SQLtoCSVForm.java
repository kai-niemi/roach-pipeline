package io.roach.pipeline.web.sql;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.web.FormModel;
import io.roach.pipeline.web.LinkRels;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonPropertyOrder({"links", "embedded"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(value = LinkRels.SQL2CSV_REL,
        collectionRelation = LinkRels.SQL2CSV_REL + LinkRels.BUNDLE_SUFFIX)
public class SQLtoCSVForm extends FormModel<SQLtoCSVForm> {
    // Source DB
    @Pattern(regexp = "jdbc:.*")
    @NotNull
    private String sourceUrl;

    private String sourceUsername;

    private String sourcePassword;

    @Min(1)
    private int chunkSize;

    @Min(0)
    private int linesToSkip;

    @Min(0)
    private int pageSize;

    @NotNull
    private String sortKeys;

    @NotNull
    private String selectClause;

    @NotNull
    private String fromClause;

    private String whereClause = "WHERE 1=1";

    private String createQuery;

    @NotNull
    private String importQuery;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUsername() {
        return sourceUsername;
    }

    public void setSourceUsername(String sourceUsername) {
        this.sourceUsername = sourceUsername;
    }

    public String getSourcePassword() {
        return sourcePassword;
    }

    public void setSourcePassword(String sourcePassword) {
        this.sourcePassword = sourcePassword;
    }

    public String getCreateQuery() {
        return createQuery;
    }

    public void setCreateQuery(String createQuery) {
        this.createQuery = createQuery;
    }

    public String getImportQuery() {
        return importQuery;
    }

    public void setImportQuery(String importQuery) {
        this.importQuery = importQuery;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getLinesToSkip() {
        return linesToSkip;
    }

    public void setLinesToSkip(int linesToSkip) {
        this.linesToSkip = linesToSkip;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortKeys() {
        return sortKeys;
    }

    public void setSortKeys(String sortKeys) {
        this.sortKeys = sortKeys;
    }

    public String getSelectClause() {
        return selectClause;
    }

    public void setSelectClause(String selectClause) {
        this.selectClause = selectClause;
    }

    public String getFromClause() {
        return fromClause;
    }

    public void setFromClause(String fromClause) {
        this.fromClause = fromClause;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }

    public DataSourceProps toSourceDataSourceProperties() {
        return DataSourceProps.builder()
                .withUrl(getSourceUrl())
                .withUsername(getSourceUsername())
                .withPassword(getSourcePassword())
                .withName("sql2csv-source")
                .build();
    }
}
