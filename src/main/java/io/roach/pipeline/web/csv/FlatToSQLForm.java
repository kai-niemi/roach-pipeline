package io.roach.pipeline.web.csv;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.item.flatfile.schema.FlatFileSchema;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.web.FormModel;
import io.roach.pipeline.web.LinkRels;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@JsonPropertyOrder({"links", "embedded"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(value = LinkRels.FLAT2SQL_REL,
        collectionRelation = LinkRels.FLAT2SQL_REL + LinkRels.BUNDLE_SUFFIX)
public class FlatToSQLForm extends FormModel<FlatToSQLForm> {
    // Target DB
    @Pattern(regexp = "jdbc:.*")
    private String targetUrl;

    private String targetUsername;

    private String targetPassword;

    @Positive
    private int concurrency;

    @NotNull
    private String sourceFile;

    private String schemaFile;

    // When using source DB introspection as template
    private FlatFileSchema schema;

    @PositiveOrZero
    private int linesToSkip;

    @Positive
    private int chunkSize;

    private String awsAccessKey;

    private String awsSecretAccessKey;

    private String awsRegion;

    private String gcsCredentials;

    private String gcsAuth = "specified";

    private String createQuery;

    private String insertQuery;

    public String getGcsCredentials() {
        return gcsCredentials;
    }

    public void setGcsCredentials(String gcsCredentials) {
        this.gcsCredentials = gcsCredentials;
    }

    public String getGcsAuth() {
        return gcsAuth;
    }

    public void setGcsAuth(String gcsAuth) {
        this.gcsAuth = gcsAuth;
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

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSchemaFile() {
        return schemaFile;
    }

    public void setSchemaFile(String schemaFile) {
        this.schemaFile = schemaFile;
    }

    public FlatFileSchema getSchema() {
        return schema;
    }

    public void setSchema(FlatFileSchema schema) {
        this.schema = schema;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretAccessKey() {
        return awsSecretAccessKey;
    }

    public void setAwsSecretAccessKey(String awsSecretAccessKey) {
        this.awsSecretAccessKey = awsSecretAccessKey;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
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

    public int getLinesToSkip() {
        return linesToSkip;
    }

    public void setLinesToSkip(int linesToSkip) {
        this.linesToSkip = linesToSkip;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public DataSourceProps toTargetDataSourceProperties() {
        return DataSourceProps.builder()
                .withName("flat2sql")
                .withUrl(targetUrl)
                .withUsername(targetUsername)
                .withPassword(targetPassword)
                .withConcurrency(concurrency)
                .build();
    }
}
