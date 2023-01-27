package io.roach.pipeline.web.sql;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import io.roach.pipeline.cloud.ResourceResolver;
import io.roach.pipeline.config.ApplicationProfiles;
import io.roach.pipeline.config.ClosableDataSource;
import io.roach.pipeline.item.BatchJobLauncher;
import io.roach.pipeline.item.BatchJobManifest;
import io.roach.pipeline.item.flatfile.FlatFileStreamWriterBuilder;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.util.SortKeysExpression;
import io.roach.pipeline.web.AbstractFormController;
import io.roach.pipeline.web.BadRequestException;
import io.roach.pipeline.web.JobConfigurationException;
import io.roach.pipeline.web.LinkRels;
import jakarta.validation.Valid;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/sql2csv")
@ApplicationProfiles.Online
public class SQLtoCSVController extends AbstractFormController<SQLtoCSVForm> {
    @Autowired
    private BatchJobLauncher batchJobLauncher;

    @Value("${pipeline.nodelocal.path}")
    private String nodeLocalPath;

    @Override
    @GetMapping(value = "/form")
    public ResponseEntity<SQLtoCSVForm> getFormTemplate(@RequestParam Map<String, String> requestParams)
            throws JobExecutionException {
        final String table = requestParams.getOrDefault("table", null);

        SQLtoCSVForm form = new SQLtoCSVForm();
        form.setSourceUrl(requestParams
                .getOrDefault("sourceUrl", templateProperties.getSource().getUrl()));
        form.setSourceUsername(requestParams
                .getOrDefault("sourceUsername", templateProperties.getSource().getUsername()));
        form.setSourcePassword(requestParams
                .getOrDefault("sourcePassword", templateProperties.getSource().getPassword()));

        form.setChunkSize(templateProperties.getChunkSize());
        form.setPageSize(32);
        form.setLinesToSkip(0);
        form.setTable(table);

        if (StringUtils.hasLength(table)) {
            Assert.hasLength(form.getSourceUrl(), "sourceUrl is required for auto-templating");

            Link formLink = linkTo(methodOn(getClass())
                    .streamFromSourceToTarget(Collections.singletonMap("params", "nodelocal:" + table + "-job.json")))
                    .withRel(LinkRels.SQL2SQL_REL)
                    .withName(LinkRels.SQL2SQL_REL);
            form.add(formLink);

            try (ClosableDataSource dataSource = dataSourceFactory.apply(DataSourceProps.builder()
                    .withUrl(form.getSourceUrl())
                    .withUsername(form.getSourceUsername())
                    .withPassword(form.getSourcePassword())
                    .withName("sql2csv-source")
                    .build())) {

                List<String> keys = new ArrayList<>();
                DatabaseInfo.listPrimaryKeys(dataSource, table)
                        .forEach(primaryKey -> keys.add(primaryKey.getColumnName()));

                List<String> columns = new ArrayList<>(DatabaseInfo.listColumns(dataSource, table).keySet());

                form.setSelectClause("SELECT " + StringUtils.collectionToCommaDelimitedString(columns));
                form.setFromClause("FROM " + table);
                form.setWhereClause("WHERE 1=1");
                form.setSortKeys(StringUtils.collectionToCommaDelimitedString(keys));
                form.setCreateQuery(DatabaseInfo.showCreateTable(dataSource, table).orElse(
                        "(Unable to introspect source template db '" + form.getSourceUrl() + "')"));
                form.setImportQuery(DatabaseInfo.createImportIntoForTable(dataSource, table, formLink.toUri())
                        .orElse("(no columns - check table name)"));
            }
            form.add(linkTo(methodOn(getClass())
                    .getFormTemplate(requestParams)).withSelfRel());
        } else {
            form.setSelectClause("<select clause>");
            form.setFromClause("<from clause>");
            form.setWhereClause("<where clause>");
            form.setSortKeys("<sort keys / primary key>");
            form.setCreateQuery("<create statement or DDL file>");

            form.add(affordances(getClass()).toLink());
        }

        return ResponseEntity.ok(form);
    }

    @Override
    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> submitForm(@Valid @RequestBody SQLtoCSVForm form) {
        final StreamingResponseBody responseBody = outputStream -> {
            try {
                processForm(form, outputStream);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(responseBody);
    }

    protected void processForm(SQLtoCSVForm form, OutputStream outputStream)
            throws JobExecutionException {
        DataSourceProps sourceDBProperties = form.toSourceDataSourceProperties();

        ClosableDataSource dataSource = dataSourceFactory.apply(sourceDBProperties);
        Map<String, Order> sortConfiguration = SortKeysExpression.parse(form.getSortKeys());

        SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
        provider.setDataSource(dataSource);
        provider.setSelectClause(form.getSelectClause());
        provider.setFromClause(form.getFromClause());
        provider.setWhereClause(form.getWhereClause());
        provider.setSortKeys(sortConfiguration);

        PagingQueryProvider queryProvider = null;
        try {
            queryProvider = provider.getObject();
        } catch (Exception e) {
            throw new JobConfigurationException(NestedExceptionUtils.getMostSpecificCause(e));
        }

        final List<String> fieldNames = new ArrayList<>();

        final JdbcPagingItemReader<List<String>> itemReader =
                new JdbcPagingItemReaderBuilder<List<String>>()
                        .dataSource(dataSource)
                        .name("sourceReader")
                        .currentItemCount(form.getLinesToSkip())
                        .pageSize(form.getPageSize())
                        .fetchSize(form.getChunkSize())
                        .queryProvider(Objects.requireNonNull(queryProvider))
                        .rowMapper((rs, rowNum) -> {
                            List<String> values = new ArrayList<>();
                            int cols = rs.getMetaData().getColumnCount();

                            if (fieldNames.isEmpty()) {
                                for (int i = 1; i <= cols; i++) {
                                    fieldNames.add(rs.getMetaData().getColumnName(i));
                                }
                            }

                            for (int i = 1; i <= cols; i++) {
                                values.add(rs.getString(i));
                            }

                            return values;
                        })
                        .build();

        try {
            itemReader.afterPropertiesSet();
        } catch (Exception e) {
            throw new JobConfigurationException(NestedExceptionUtils.getMostSpecificCause(e));
        }

        final ItemWriter<Map<String, Object>> itemWriter =
                FlatFileStreamWriterBuilder.instance()
                        .setFieldNames(fieldNames)
                        .setOutputWriter(new OutputStreamWriter(new BufferedOutputStream(outputStream)))
                        .build();

        BatchJobManifest batchJobManifest = BatchJobManifest.builder()
                .withRandomId()
                .withName("sql2csv")
                .withJobParameters(builder -> builder
                        .addString("table", form.getTable()))
                .withChunkSize(form.getChunkSize())
                .withRestartExecutionId(form.getRestartExecutionId())
                .withFaultTolerance()
                .withConcurrency(1) // Blocking execution
                .build();

        batchJobLauncher.submitJob(batchJobManifest, itemReader, itemWriter); // Blocks until completion
    }

    /**
     * Needed since IMPORT INTO only use GET not POST.
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> streamFromSourceToTarget(
            @RequestParam(required = false) Map<String, String> valueMap) {
        final Map<String, String> allParams = Objects.requireNonNull(valueMap, "params required");
        allParams.put(ResourceResolver.NODE_LOCAL_PATH, nodeLocalPath);

        SQLtoCSVForm form;

        String paramsFile = allParams.get("params");
        if (paramsFile != null) {
            Resource resource = ResourceResolver.getResource(paramsFile, allParams);
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                form = new GsonBuilder().create().fromJson(reader, SQLtoCSVForm.class);
            } catch (IOException | JsonParseException e) {
                throw new BadRequestException("Error reading json params from: " + paramsFile, e);
            }
        } else {
            String sourceUrl = allParams.get("sourceUrl");
            String sourceUsername = allParams.get("sourceUsername");
            String sourcePassword = allParams.get("sourcePassword");
            String sortKeys = allParams.getOrDefault("sortKeys", "");
            String selectClause = allParams.get("selectClause");
            String fromClause = allParams.get("fromClause");
            String whereClause = allParams.get("whereClause");

            Assert.notNull(sourceUrl, "sourceUrl is null");
            Assert.notNull(selectClause, "selectClause is null");
            Assert.notNull(fromClause, "fromClause is null");

            form = new SQLtoCSVForm();
            form.setSourceUrl(sourceUrl);
            form.setSourceUsername(sourceUsername);
            form.setSourcePassword(sourcePassword);
            form.setChunkSize(Integer.parseInt(allParams.getOrDefault("chunkSize", "256")));
            form.setPageSize(Integer.parseInt(allParams.getOrDefault("pageSize", "32")));
            form.setLinesToSkip(Integer.parseInt(allParams.getOrDefault("linesToSkip", "0")));
            form.setFromClause(fromClause);
            form.setWhereClause(whereClause);
            form.setSortKeys(sortKeys);
        }

        return this.submitForm(form);
    }
}
