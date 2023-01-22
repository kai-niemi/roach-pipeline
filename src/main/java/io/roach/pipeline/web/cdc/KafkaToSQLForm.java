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
@Relation(value = LinkRels.KAFKA2SQL_REL,
        collectionRelation = LinkRels.KAFKA2SQL_REL + LinkRels.BUNDLE_SUFFIX)
public class KafkaToSQLForm extends FormModel<KafkaToSQLForm> {
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

    @Min(10)
    private int pollTimeoutSeconds = 60 * 2;

    private String createQuery;

    @NotNull
    private String insertQuery;

    @NotNull
    private String deleteQuery;

    @NotNull
    private String name;

    @NotNull
    private String topic;

    @NotNull
    private String bootstrapServers;

    @NotNull
    private String groupId;

    private boolean saveOffsets = true;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isSaveOffsets() {
        return saveOffsets;
    }

    public void setSaveOffsets(boolean saveOffsets) {
        this.saveOffsets = saveOffsets;
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

    public DataSourceProps toTargetDataSourceProperties() {
        return DataSourceProps.builder()
                .withName("kafka2sql")
                .withUrl(targetUrl)
                .withUsername(targetUsername)
                .withPassword(targetPassword)
                .withConcurrency(concurrency)
                .build();
    }
}
