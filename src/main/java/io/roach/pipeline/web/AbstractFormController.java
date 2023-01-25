package io.roach.pipeline.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.roach.pipeline.config.ClosableDataSource;
import io.roach.pipeline.config.TemplateProperties;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.util.graph.Graph;
import jakarta.servlet.http.HttpServletResponse;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public abstract class AbstractFormController<T extends FormModel<? extends T>> extends RepresentationModel<T>
        implements FormController<T> {
    public static <F extends FormController<?>> Affordances affordances(Class<F> controllerClass) throws
            JobExecutionException {
        return Affordances.of(linkTo(methodOn(controllerClass)
                .getFormTemplate(null)).withSelfRel()
                .andAffordance(afford(methodOn(controllerClass).submitForm(null))));
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected Function<DataSourceProps, ClosableDataSource> dataSourceFactory;

    @Autowired
    protected TemplateProperties templateProperties;

    @Autowired
    @Qualifier("prettyObjectMapper")
    private ObjectMapper objectMapper;

    @Override
    @GetMapping(value = {"/forms"})
    public ResponseEntity<CollectionModel<T>> getFormTemplateBundle(@RequestParam Map<String, String> requestParams) {
        String sourceUrl = requestParams
                .getOrDefault("sourceUrl", templateProperties.getSource().getUrl());
        String sourceUsername = requestParams
                .getOrDefault("sourceUsername", templateProperties.getSource().getUsername());
        String sourcePassword = requestParams
                .getOrDefault("sourcePassword", templateProperties.getSource().getPassword());
        String schema = requestParams
                .getOrDefault("schema", "public");

        Assert.hasLength(sourceUrl, "sourceUrl is required for auto-templating");

        try (ClosableDataSource dataSource = dataSourceFactory.apply(
                DataSourceProps.builder()
                        .withUrl(sourceUrl)
                        .withUsername(sourceUsername)
                        .withPassword(sourcePassword)
                        .withName("template-source")
                        .build())) {

            Map<String, T> forms = new HashMap<>();

            Graph<String, DatabaseInfo.ForeignKey> graph = new Graph<>();

            DatabaseInfo.listTables(dataSource, schema).forEach(table -> {
                graph.addNode(table);

                DatabaseInfo.listForeignKeys(dataSource, table).forEach(foreignKey -> {
                    graph.addNode(foreignKey.getPkTableName());
                    graph.addEdge(table, foreignKey.getPkTableName(), foreignKey);
                });

                try {
                    T form = getFormTemplate(requestParams).getBody();
                    form.setTable(table);
                    forms.put(table, form);
                } catch (JobExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

            List<T> orderedForms = new ArrayList<>();
            try {
                graph.topologicalSort(true).forEach(t -> orderedForms.add(forms.get(t)));
            } catch (IllegalStateException e) {
                orderedForms.addAll(forms.values());
                logger.warn("", e);
            }

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                    .body(CollectionModel.of(orderedForms)
                            .add(linkTo(methodOn(getClass())
                                    .getFormTemplateBundle(requestParams))
                                    .withSelfRel()));
        }
    }

    @Override
    @GetMapping(value = {"/forms/zip"}, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> getFormTemplateZipBundle(
            @RequestParam Map<String, String> requestParams, HttpServletResponse response) {

        CollectionModel<T> collectionModel = getFormTemplateBundle(requestParams).getBody();

        StreamingResponseBody streamingResponseBody = out -> {
            try (final ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
                 final BufferedWriter zipWriter = new BufferedWriter(new OutputStreamWriter(zipOutputStream))) {

                int bytes = 0;
                for (T form : Objects.requireNonNull(collectionModel)) {
                    Assert.notNull(form.getTable(), "table name required");

                    ZipEntry zipEntry = new ZipEntry(form.getTable() + ".json");
                    zipOutputStream.putNextEntry(zipEntry);

                    zipWriter.write(objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(form));
                    zipWriter.flush();
                    zipOutputStream.closeEntry();

                    bytes += zipEntry.getSize();
                }

                response.setContentLength(bytes);
            } catch (IOException e) {
                logger.error("I/O exception while streaming data", e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=form-bundle.zip")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(streamingResponseBody);
    }

}
