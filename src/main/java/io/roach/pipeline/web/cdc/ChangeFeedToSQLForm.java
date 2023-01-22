package io.roach.pipeline.web.cdc;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.web.FormModel;
import io.roach.pipeline.web.LinkRels;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonPropertyOrder({"links", "embedded"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(value = LinkRels.CDC2SQL_REL,
        collectionRelation = LinkRels.CDC2SQL_REL + LinkRels.BUNDLE_SUFFIX)
public class ChangeFeedToSQLForm extends FormModel<ChangeFeedToSQLForm> {
    // Source DB
    @Pattern(regexp = "jdbc:.*")
    @NotNull
    private String sourceUrl;

    private String sourceUsername;

    private String sourcePassword;

    private String changeFeedQuery;

    // Target DB
    @Pattern(regexp = "jdbc:.*")
    private String targetUrl;

    private String targetUsername;

    private String targetPassword;

    @Min(1)
    @Max(256)
    private int concurrency;

    @NotNull
    private String subscriberId;

    @Min(1)
    private int chunkSize;

    @Min(10)
    private int pollTimeoutSeconds;

    // Target schema
    private String createQuery;

    @NotNull
    private String insertQuery;

    @NotNull
    private String deleteQuery;

    public String getChangeFeedQuery() {
        return changeFeedQuery;
    }

    public void setChangeFeedQuery(String changeFeedQuery) {
        this.changeFeedQuery = changeFeedQuery;
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

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

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

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getPollTimeoutSeconds() {
        return pollTimeoutSeconds;
    }

    public void setPollTimeoutSeconds(int pollTimeoutSeconds) {
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }

    public String getCreateQuery() {
        return createQuery;
    }

    public void setCreateQuery(String createQuery) {
        this.createQuery = createQuery;
    }

    public String getInsertQuery() {
        return insertQuery;
    }

    public void setInsertQuery(String insertQuery) {
        this.insertQuery = insertQuery;
    }

    public String getDeleteQuery() {
        return deleteQuery;
    }

    public void setDeleteQuery(String deleteQuery) {
        this.deleteQuery = deleteQuery;
    }

    public DataSourceProps toSourceDataSourceProperties() {
        return DataSourceProps.builder()
                .withName("cdc2sql-source")
                .withUrl(sourceUrl)
                .withUsername(sourceUsername)
                .withPassword(sourcePassword)
                .withConcurrency(concurrency)
                .build();
    }

    public DataSourceProps toTargetDataSourceProperties() {
        return DataSourceProps.builder()
                .withName("cdc2sql-target")
                .withUrl(targetUrl)
                .withUsername(targetUsername)
                .withPassword(targetPassword)
                .withConcurrency(concurrency)
                .build();
    }
}


