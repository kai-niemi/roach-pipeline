package io.roach.pipeline.web.csv;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.roach.pipeline.cloud.GCSBucketResource;
import io.roach.pipeline.cloud.ResourceResolver;
import io.roach.pipeline.cloud.S3BucketResource;
import io.roach.pipeline.config.ApplicationProfiles;
import io.roach.pipeline.config.ClosableDataSource;
import io.roach.pipeline.item.BatchJobLauncher;
import io.roach.pipeline.item.BatchJobManifest;
import io.roach.pipeline.item.flatfile.FlatFileReaderBuilder;
import io.roach.pipeline.item.flatfile.schema.FlatFileSchema;
import io.roach.pipeline.item.flatfile.schema.FlatFileSchemaUtils;
import io.roach.pipeline.item.jdbc.NamedParameterItemWriter;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.web.AbstractFormController;
import io.roach.pipeline.web.BadRequestException;
import io.roach.pipeline.web.LinkRels;
import io.roach.pipeline.web.MessageModel;
import io.roach.pipeline.web.admin.JobController;
import jakarta.validation.Valid;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@RestController
@RequestMapping("/flat2sql")
@ApplicationProfiles.Online
public class FlatToSQLController extends AbstractFormController<FlatToSQLForm> {
    @Autowired
    private BatchJobLauncher batchJobLauncher;

    @Value("${pipeline.nodelocal.path}")
    private String nodeLocalPath;

    @Override
    @GetMapping(value = "/form")
    public ResponseEntity<FlatToSQLForm> getFormTemplate(@RequestParam Map<String, String> requestParams)
            throws JobExecutionException {
        final String table = requestParams.getOrDefault("table", null);

        FlatToSQLForm form = new FlatToSQLForm();
        form.setConcurrency(Runtime.getRuntime().availableProcessors());
        form.setChunkSize(templateProperties.getChunkSize());
        form.setLinesToSkip(0);

        form.setTargetUrl(requestParams
                .getOrDefault("targetUrl", templateProperties.getTarget().getUrl()));
        form.setTargetUsername(requestParams
                .getOrDefault("targetUsername", templateProperties.getTarget().getUsername()));
        form.setTargetPassword(requestParams
                .getOrDefault("targetPassword", templateProperties.getTarget().getPassword()));

        if (StringUtils.hasLength(table)) {
            String sourceUrl = requestParams
                    .getOrDefault("sourceUrl", templateProperties.getSource().getUrl());
            String sourceUsername = requestParams
                    .getOrDefault("sourceUsername", templateProperties.getSource().getUsername());
            String sourcePassword = requestParams
                    .getOrDefault("sourcePassword", templateProperties.getSource().getPassword());

            Assert.hasLength(sourceUrl, "sourceUrl is required for auto-templating");

            form.setSourceFile("nodelocal:" + table + "-data.csv");

            try (ClosableDataSource dataSource = dataSourceFactory.apply(DataSourceProps.builder()
                    .withUrl(sourceUrl)
                    .withUsername(sourceUsername)
                    .withPassword(sourcePassword)
                    .withName("flat2sql-source")
                    .build())) {
                form.setSchema(
                        FlatFileSchemaUtils.generateSchema(dataSource, table, ",", field -> true));
                form.setInsertQuery(DatabaseInfo.createUpsertForTable(dataSource, table)
                        .orElse("(table '" + table + "' not found)"));
                form.setCreateQuery(DatabaseInfo.showCreateTable(dataSource, table).orElse(
                        "(Unable to introspect source template db '" + sourceUrl + "')"));
            }
            form.add(linkTo(methodOn(getClass())
                    .getFormTemplate(requestParams)).withSelfRel());
        } else {
            form.setSourceFile("<source CSV or fixed-width file path>");
            form.setSchemaFile("<JSON schema file path>");
            form.setCreateQuery("<create statement or DDL file>");
            form.setInsertQuery("<insert or upsert statement with named parameters>");

            form.add(Link.of(fromCurrentContextPath()
                            .pathSegment("templates", "customers-data.csv")
                            .toUriString())
                    .withRel(LinkRels.TEMPLATE_REL)
                    .withName("customers-data.csv")
                    .withType(MediaType.TEXT_PLAIN_VALUE)
                    .withTitle("Sample customers CSV import file (CSV)"));
            form.add(Link.of(fromCurrentContextPath()
                            .pathSegment("templates", "customers-schema.json")
                            .toUriString())
                    .withRel(LinkRels.TEMPLATE_REL)
                    .withName("customers-schema.json")
                    .withTitle("Sample customers CSV file JSON schema (JSON)"));
            form.add(Link.of(fromCurrentContextPath()
                            .pathSegment("templates", "customers-ddl.sql")
                            .toUriString())
                    .withRel(LinkRels.TEMPLATE_REL)
                    .withName("customers-ddl.sql")
                    .withType(MediaType.TEXT_PLAIN_VALUE)
                    .withTitle("Sample customers table schema (SQL)"));

            form.add(affordances(getClass()).toLink());
        }

        return ResponseEntity.ok(form);
    }

    @Override
    @PostMapping
    public ResponseEntity<?> submitForm(@Valid @RequestBody FlatToSQLForm form)
            throws JobExecutionException {
        Map<String, String> allParams = new HashMap<>();
        allParams.put(S3BucketResource.AWS_ACCESS_KEY_ID, form.getAwsAccessKey());
        allParams.put(S3BucketResource.AWS_SECRET_ACCESS_KEY, form.getAwsSecretAccessKey());
        allParams.put(S3BucketResource.AWS_DEFAULT_REGION, form.getAwsRegion());

        allParams.put(GCSBucketResource.AUTH, form.getGcsAuth());
        allParams.put(GCSBucketResource.CREDENTIALS, form.getGcsCredentials());

        allParams.put(ResourceResolver.NODE_LOCAL_PATH, nodeLocalPath);

        FlatFileSchema flatFileSchema = form.getSchema();
        if (flatFileSchema == null) {
            final String schemaFile = form.getSchemaFile();
            if (schemaFile == null) {
                throw new BadRequestException("Missing required param [schemaFile]");
            }
            try {
                Resource schemaResource = ResourceResolver.getResource(schemaFile, allParams);
                flatFileSchema = FlatFileSchemaUtils.readFromStream(schemaResource.getInputStream());
            } catch (IOException e) {
                throw new BadRequestException("Error configuring source schema", e);
            }
        }

        final String sourceFile = form.getSourceFile();
        if (sourceFile == null) {
            throw new BadRequestException("Missing required param [sourceFile]");
        }

        final Resource sourceResource = ResourceResolver.getResource(sourceFile, allParams);

        final ItemReader<Map<String, Object>> itemReader = FlatFileReaderBuilder.instance()
                .setFlatFileSchema(flatFileSchema)
                .setInputResource(sourceResource)
                .setLinesToSkip(form.getLinesToSkip())
                .build();

        ClosableDataSource targetDS = dataSourceFactory.apply(form.toTargetDataSourceProperties());
        final ItemWriter<Map<String, Object>> itemWriter = NamedParameterItemWriter.builder()
                .setDataSource(targetDS)
                .setCreateQuery(form.getCreateQuery())
                .setUpdateQuery(form.getInsertQuery())
                .build();

        BatchJobManifest batchJobManifest = BatchJobManifest.builder()
                .withRandomId()
                .withName("flat2sql")
                .withJobParameters(builder -> builder
                        .addString("table", form.getTable()))
                .withChunkSize(form.getChunkSize())
                .withRestartExecutionId(form.getRestartExecutionId())
                .withFaultTolerance()
                .withConcurrency(form.getConcurrency())
                .build();

        batchJobLauncher.submitJob(batchJobManifest, itemReader, itemWriter);

        MessageModel messageModel = MessageModel.from("Flat2SQL Job Accepted");
        messageModel.add(WebMvcLinkBuilder.linkTo(methodOn(JobController.class)
                        .getFutureJobExecution(batchJobManifest.getId()))
                .withRel(LinkRels.JOB_EXECUTION_REL));

        return ResponseEntity.accepted()
                .cacheControl(CacheControl.empty())
                .body(messageModel);
    }
}
