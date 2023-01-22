package io.roach.pipeline.web.sql;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.web.LinkRels;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.web.FormModel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonPropertyOrder({"links", "embedded"})
@JsonIgnoreProperties(value = {"links"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(value = LinkRels.SQL2SQL_REL,
        collectionRelation = LinkRels.CDC2SQL_REL + LinkRels.BUNDLE_SUFFIX)
public class SQLtoSQLForm extends FormModel<SQLtoSQLForm> {
    // Source DB
    @Pattern(regexp = "jdbc:.*")
    @NotNull
    private String sourceUrl;

    private String sourceUsername;

    private String sourcePassword;

    // Target DB
    @Pattern(regexp = "jdbc:.*")
    @NotNull
    private String targetUrl;

    private String targetUsername;

    private String targetPassword;

    @Min(1)
    @Max(256)
    private int concurrency;

    @Min(1)
    private int chunkSize;

    @Min(0)
    private int linesToSkip;

    @Min(1)
    private int pageSize;

    @NotNull
    private String sortKeys;

    @NotNull
    private String selectClause;

    @NotNull
    private String fromClause;

    private String whereClause = "WHERE 1=1";

    @NotNull
    private String insertQuery;

    private String createQuery;

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getTargetPassword() {
        return targetPassword;
    }

    public void setTargetPassword(String targetPassword) {
        this.targetPassword = targetPassword;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public String getInsertQuery() {
        return insertQuery;
    }

    public void setInsertQuery(String insertQuery) {
        this.insertQuery = insertQuery;
    }

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

    public String getCreateQuery() {
        return createQuery;
    }

    public void setCreateQuery(String createQuery) {
        this.createQuery = createQuery;
    }

    public DataSourceProps toSourceDataSourceProperties() {
        return DataSourceProps.builder()
                .withName("sql2sql-source")
                .withUrl(sourceUrl)
                .withUsername(sourceUsername)
                .withPassword(sourcePassword)
                .withConcurrency(concurrency)
                .build();
    }

    public DataSourceProps toTargetDataSourceProperties() {
        return DataSourceProps.builder()
                .withName("sql2sql-source")
                .withUrl(targetUrl)
                .withUsername(targetUsername)
                .withPassword(targetPassword)
                .withConcurrency(concurrency)
                .build();
    }
}
