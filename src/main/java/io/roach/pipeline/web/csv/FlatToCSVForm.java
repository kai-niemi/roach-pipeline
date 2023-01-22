package io.roach.pipeline.web.csv;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.web.FormModel;
import io.roach.pipeline.web.LinkRels;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonPropertyOrder({"links", "embedded"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(value = LinkRels.FLAT2CSV_REL,
        collectionRelation = LinkRels.FLAT2CSV_REL + LinkRels.BUNDLE_SUFFIX)
public class FlatToCSVForm extends FormModel<FlatToCSVForm> {
    @Min(1)
    @Max(256)
    private int concurrency;

    @NotNull
    private String sourceFile;

    @NotNull
    private String schemaFile;

    @Min(1)
    private int chunkSize;

    @Min(0)
    private int linesToSkip;

    private String awsAccessKey;

    private String awsSecretAccessKey;

    private String awsRegion;

    private String gcsCredentials;

    private String gcsAuth = "specified";

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

    @Override
    @JsonIgnore
    public String getTable() {
        return super.getTable();
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

//    public int getConcurrency() {
//        return concurrency;
//    }
//
//    public void setConcurrency(int concurrency) {
//        this.concurrency = concurrency;
//    }
}
