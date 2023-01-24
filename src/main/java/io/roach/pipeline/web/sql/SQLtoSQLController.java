package io.roach.pipeline.web.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
import org.springframework.core.NestedExceptionUtils;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.roach.pipeline.config.ApplicationProfiles;
import io.roach.pipeline.config.ClosableDataSource;
import io.roach.pipeline.item.BatchJobLauncher;
import io.roach.pipeline.item.BatchJobManifest;
import io.roach.pipeline.item.jdbc.NamedParameterItemWriter;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.util.SortKeysExpression;
import io.roach.pipeline.web.AbstractFormController;
import io.roach.pipeline.web.JobConfigurationException;
import io.roach.pipeline.web.LinkRels;
import io.roach.pipeline.web.MessageModel;
import io.roach.pipeline.web.admin.JobController;
import jakarta.validation.Valid;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@RestController
@RequestMapping("/sql2sql")
@ApplicationProfiles.Online
public class SQLtoSQLController extends AbstractFormController<SQLtoSQLForm> {
    @Autowired
    private BatchJobLauncher batchJobLauncher;

    @Override
    @GetMapping(value = "/form")
    public ResponseEntity<SQLtoSQLForm> getFormTemplate(@RequestParam Map<String, String> requestParams)
            throws JobExecutionException {
        final String table = requestParams.getOrDefault("table", null);

        SQLtoSQLForm form = new SQLtoSQLForm();
        form.setConcurrency(Runtime.getRuntime().availableProcessors());
        form.setTable(table);
        form.setChunkSize(templateProperties.getChunkSize());
        form.setPageSize(32);
        form.setLinesToSkip(0);

        form.setSourceUrl(requestParams
                .getOrDefault("sourceUrl", templateProperties.getSource().getUrl()));
        form.setSourceUsername(requestParams
                .getOrDefault("sourceUsername", templateProperties.getSource().getUsername()));
        form.setSourcePassword(requestParams
                .getOrDefault("sourcePassword", templateProperties.getSource().getPassword()));

        form.setTargetUrl(requestParams
                .getOrDefault("targetUrl", templateProperties.getTarget().getUrl()));
        form.setTargetUsername(requestParams
                .getOrDefault("targetUsername", templateProperties.getTarget().getUsername()));
        form.setTargetPassword(requestParams
                .getOrDefault("targetPassword", templateProperties.getTarget().getPassword()));

        form.setSelectClause("SELECT *");
        form.setWhereClause("WHERE 1=1");

        if (StringUtils.hasLength(table)) {
            Assert.hasLength(form.getSourceUrl(), "sourceUrl is required for auto-templating");

            form.setFromClause("FROM " + table);

            try (ClosableDataSource dataSource = dataSourceFactory.apply(DataSourceProps.builder()
                    .withUrl(form.getSourceUrl())
                    .withUsername(form.getSourceUsername())
                    .withPassword(form.getSourcePassword())
                    .withName("sql2sql-source")
                    .build())) {
                List<String> keys = new ArrayList<>();
                DatabaseInfo.listPrimaryKeys(dataSource, table).forEach(primaryKey -> {
                    keys.add(primaryKey.getColumnName() + " ASC");
                });
                form.setSortKeys(StringUtils.collectionToCommaDelimitedString(keys));
                form.setInsertQuery(DatabaseInfo.createUpsertForTable(dataSource, table)
                        .orElse("(table " + table + " not found)"));
                form.setCreateQuery(DatabaseInfo.showCreateTable(dataSource, table).orElse(
                        "(unable to introspect - template source '" + form.getSourceUrl() + "' is not CockroachDB)"));
            }
            form.add(linkTo(methodOn(getClass())
                    .getFormTemplate(requestParams)).withSelfRel());
        } else {
            form.setSelectClause("<select clause>");
            form.setFromClause("<from clause>");
            form.setWhereClause("<where clause>");
            form.setSortKeys("<sort keys / primary key>");
            form.setCreateQuery("<create statement or DDL file>");
            form.setInsertQuery("<insert or upsert statement with named parameters>");

            form.add(Link.of(fromCurrentContextPath()
                            .pathSegment("templates", "customers-ddl.sql")
                            .toUriString())
                    .withRel(LinkRels.TEMPLATE_REL)
                    .withTitle("Sample customers DDL (SQL)"));

            form.add(affordances(getClass()).toLink());
        }

        return ResponseEntity.ok(form);
    }

    @PostMapping
    public ResponseEntity<MessageModel> submitForm(@Valid @RequestBody SQLtoSQLForm form)
            throws JobExecutionException {
        DataSourceProps sourceDBProperties = form.toSourceDataSourceProperties();
        DataSourceProps targetDBProperties = form.toTargetDataSourceProperties();

        Map<String, Order> sortConfiguration = SortKeysExpression.parse(form.getSortKeys());

        ClosableDataSource sourceDS = dataSourceFactory.apply(sourceDBProperties);
        ClosableDataSource targetDS = dataSourceFactory.apply(targetDBProperties);

        PagingQueryProvider queryProvider;
        try {
            SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
            provider.setDataSource(sourceDS);
            provider.setSelectClause(form.getSelectClause());
            provider.setFromClause(form.getFromClause());
            provider.setWhereClause(form.getWhereClause());
            provider.setSortKeys(sortConfiguration);
            queryProvider = provider.getObject();
        } catch (Exception e) {
            throw new JobConfigurationException(NestedExceptionUtils.getMostSpecificCause(e));
        }

        final JdbcPagingItemReader<Map<String, Object>> itemReader =
                new JdbcPagingItemReaderBuilder<Map<String, Object>>()
                        .dataSource(sourceDS)
                        .name("sql2sql_paging_reader")
                        .fetchSize(form.getChunkSize())
                        .currentItemCount(form.getLinesToSkip())
                        .pageSize(form.getPageSize())
                        .saveState(true)
                        .queryProvider(Objects.requireNonNull(queryProvider))
                        .rowMapper((rs, rowNum) -> {
                            Map<String, Object> values = new HashMap<>();
                            int cols = rs.getMetaData().getColumnCount();
                            for (int i = 1; i <= cols; i++) {
                                String name = rs.getMetaData().getColumnName(i);
                                values.put(name, rs.getObject(i));
                            }
                            return values;
                        })
                        .build();

        try {
            itemReader.afterPropertiesSet();
        } catch (Exception e) {
            throw new JobConfigurationException(e);
        }

        final ItemWriter<Map<String, Object>> itemWriter = NamedParameterItemWriter.builder()
                .setDataSource(targetDS)
                .setCreateQuery(form.getCreateQuery())
                .setUpdateQuery(form.getInsertQuery())
                .build();

        BatchJobManifest batchJobManifest = BatchJobManifest.builder()
                .withRandomId()
                .withName("sql2sql")
                .withJobParameters(builder -> builder
                        .addString("table", form.getTable()))
                .withChunkSize(form.getChunkSize())
                .withRestartExecutionId(form.getRestartExecutionId())
                .withFaultTolerance()
                .withConcurrency(form.getConcurrency())
                .build();

        batchJobLauncher.submitJob(batchJobManifest, itemReader, itemWriter);

        MessageModel messageModel = MessageModel.from("SQL2SQL Job Accepted");
        messageModel.add(WebMvcLinkBuilder.linkTo(methodOn(JobController.class)
                        .getFutureJobExecution(batchJobManifest.getId()))
                .withRel(LinkRels.JOB_EXECUTION_REL));

        return ResponseEntity.accepted().body(messageModel);
    }

    @PostMapping(value = {"/forms"})
    public ResponseEntity<CollectionModel<MessageModel>> submitFormTemplates(
            @RequestBody CollectionModel<SQLtoSQLForm> bundle,
            @RequestParam(value = "order", required = false) String order)
            throws JobExecutionException {
        Collection<SQLtoSQLForm> forms = new ArrayList<>(bundle.getContent());
        List<SQLtoSQLForm> orderedForms = new ArrayList<>();
        List<String> tablesInOrder
                = Arrays.stream(StringUtils.commaDelimitedListToStringArray(order)).toList();

        if (!tablesInOrder.isEmpty()) {
            tablesInOrder.forEach(table -> {
                SQLtoSQLForm match
                        = forms.stream().filter(form -> table.equals(form.getTable())).findFirst()
                        .orElseThrow(() -> new JobConfigurationException("No such table exist: " + table));
                orderedForms.add(match);
            });
        } else {
            orderedForms.addAll(forms);
        }

        List<MessageModel> models = new ArrayList<>();

        for (SQLtoSQLForm form : orderedForms) {
            logger.info("Submitting form for table: {}", form.getTable());
            ResponseEntity<MessageModel> responseEntity = submitForm(form);
            MessageModel responseBody = responseEntity.getBody();
            models.add(responseBody);
        }

        // FK topology order for tpc-c
        // warehouse,district,customer,history,"order",new_order,item,stock,order_line

        return ResponseEntity.accepted().body(CollectionModel.of(models));
    }

}
