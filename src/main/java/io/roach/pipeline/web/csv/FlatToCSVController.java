package io.roach.pipeline.web.csv;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import io.roach.pipeline.cloud.GCSBucketResource;
import io.roach.pipeline.cloud.ResourceResolver;
import io.roach.pipeline.cloud.S3BucketResource;
import io.roach.pipeline.config.ApplicationProfiles;
import io.roach.pipeline.item.BatchJobLauncher;
import io.roach.pipeline.item.BatchJobManifest;
import io.roach.pipeline.item.flatfile.FlatFileReaderBuilder;
import io.roach.pipeline.item.flatfile.FlatFileStreamWriterBuilder;
import io.roach.pipeline.item.flatfile.schema.FlatFileSchema;
import io.roach.pipeline.item.flatfile.schema.FlatFileSchemaUtils;
import io.roach.pipeline.web.AbstractFormController;
import io.roach.pipeline.web.BadRequestException;
import io.roach.pipeline.web.LinkRels;
import jakarta.validation.Valid;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@RestController
@RequestMapping("/flat2csv")
@ApplicationProfiles.Online
public class FlatToCSVController extends AbstractFormController<FlatToCSVForm> {
    @Autowired
    private BatchJobLauncher batchJobLauncher;

    @Value("${pipeline.nodelocal.path}")
    private String nodeLocalPath;

    @Override
    @GetMapping(value = "/form")
    public ResponseEntity<FlatToCSVForm> getFormTemplate(@RequestParam Map<String, String> requestParams)
            throws JobExecutionException {
        FlatToCSVForm form = new FlatToCSVForm();
        form.setSourceFile("<source CSV or fixed-width file path>");
        form.setSchemaFile("<JSON schema file path>");
        form.setChunkSize(256);
        form.setLinesToSkip(0);

        form.add(Link.of(fromCurrentContextPath()
                        .pathSegment("templates", "products.txt")
                        .toUriString())
                .withRel(LinkRels.TEMPLATE_REL)
                .withTitle("Sample products import file (TXT)"));
        form.add(Link.of(fromCurrentContextPath()
                        .pathSegment("templates", "products-schema.json")
                        .toUriString())
                .withRel(LinkRels.TEMPLATE_REL)
                .withTitle("Sample products schema (JSON)"));

        form.add(affordances(getClass()).toLink());

        return ResponseEntity.ok(form);
    }

    @Override
    @PostMapping
    public ResponseEntity<StreamingResponseBody> submitForm(@Valid @RequestBody FlatToCSVForm form) {
        final StreamingResponseBody responseBody = outputStream -> {
            try {
                processForm(form, outputStream);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(responseBody);
    }

    private void processForm(FlatToCSVForm form, OutputStream outputStream) throws IOException, JobExecutionException {
        final String source = form.getSourceFile();
        if (source == null) {
            throw new BadRequestException("Missing required param [source]");
        }
        final String schema = form.getSchemaFile();
        if (schema == null) {
            throw new BadRequestException("Missing required param [schema]");
        }

        Map<String, String> allParams = new HashMap<>();
        allParams.put(S3BucketResource.AWS_ACCESS_KEY_ID, form.getAwsAccessKey());
        allParams.put(S3BucketResource.AWS_SECRET_ACCESS_KEY, form.getAwsSecretAccessKey());
        allParams.put(S3BucketResource.AWS_DEFAULT_REGION, form.getAwsRegion());

        allParams.put(GCSBucketResource.AUTH, form.getGcsAuth());
        allParams.put(GCSBucketResource.CREDENTIALS, form.getGcsCredentials());

        allParams.put(ResourceResolver.NODE_LOCAL_PATH, nodeLocalPath);

        final Resource sourceResource = ResourceResolver.getResource(source, allParams);
        final Resource schemaResource = ResourceResolver.getResource(schema, allParams);
        final int linesToSkip = form.getLinesToSkip();
        final int chunkSize = form.getChunkSize();

        final FlatFileSchema flatFileSchema =
                FlatFileSchemaUtils.readFromStream(schemaResource.getInputStream());

        final ItemReader<Map<String, Object>> itemReader = FlatFileReaderBuilder.instance()
                .setFlatFileSchema(flatFileSchema)
                .setInputResource(sourceResource)
                .setLinesToSkip(linesToSkip)
                .build();

        final ItemWriter<Map<String, Object>> itemWriter = FlatFileStreamWriterBuilder.instance()
                .setFieldNames(flatFileSchema.allFieldNames())
                .setOutputWriter(new OutputStreamWriter(new BufferedOutputStream(outputStream)))
                .build();

        BatchJobManifest batchJobManifest = BatchJobManifest.builder()
                .withRandomId()
                .withName("flat2csv")
                .withJobParameters(builder -> builder
                        .addString("table", form.getTable()))
                .withChunkSize(chunkSize)
                .withRestartExecutionId(form.getRestartExecutionId())
                .withConcurrency(1) // Blocking execution
                .build();

        batchJobLauncher.submitJob(batchJobManifest, itemReader, itemWriter,
                BatchJobLauncher.passThroughItemProcessor(),
                BatchJobLauncher.loggingReadListener(sourceResource),
                BatchJobLauncher.loggingWriteListener()
        );
    }

    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamFromSourceToTarget(
            @RequestParam(required = false) MultiValueMap<String, String> valueMap) {
        final Map<String, String> allParams = Objects.requireNonNull(valueMap, "params required").toSingleValueMap();
        allParams.put(ResourceResolver.NODE_LOCAL_PATH, nodeLocalPath);

        FlatToCSVForm form;

        String paramsFile = allParams.get("params");
        if (paramsFile != null) {
            Resource resource = ResourceResolver.getResource(paramsFile, allParams);
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                form = new GsonBuilder().create().fromJson(reader, FlatToCSVForm.class);
            } catch (IOException | JsonParseException e) {
                throw new BadRequestException("Error reading json params from: " + paramsFile, e);
            }
        } else {
            String source = allParams.get("source");
            Assert.notNull(source, "source is null");

            String schema = allParams.get("schema");
            Assert.notNull(schema, "schema is null");

            form = new FlatToCSVForm();
            form.setSourceFile(source);
            form.setSchemaFile(schema);
            form.setChunkSize(Integer.parseInt(allParams.getOrDefault("chunkSize", "256")));
            form.setLinesToSkip(Integer.parseInt(allParams.getOrDefault("linesToSkip", "0")));
            form.setAwsAccessKey(allParams.getOrDefault("AWS_ACCESS_KEY_ID", ""));
            form.setAwsSecretAccessKey(allParams.getOrDefault("AWS_SECRET_ACCESS_KEY", ""));
            form.setAwsRegion(allParams.getOrDefault("AWS_DEFAULT_REGION", ""));
        }

        return this.submitForm(form);
    }
}
