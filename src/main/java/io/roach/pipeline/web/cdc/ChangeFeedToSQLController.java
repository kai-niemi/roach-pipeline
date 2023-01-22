package io.roach.pipeline.web.cdc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
import io.roach.pipeline.item.changefeed.ChangeFeedDispatcher;
import io.roach.pipeline.item.changefeed.ChangeFeedItemReader;
import io.roach.pipeline.item.changefeed.CompositeItemWriter;
import io.roach.pipeline.item.changefeed.Subscriber;
import io.roach.pipeline.item.jdbc.NamedParameterItemWriter;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.AddressUtils;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.web.AbstractFormController;
import io.roach.pipeline.web.JobConfigurationException;
import io.roach.pipeline.web.LinkRels;
import io.roach.pipeline.web.admin.JobController;
import jakarta.validation.Valid;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@RestController
@RequestMapping("/cdc2sql")
@ApplicationProfiles.Online
public class ChangeFeedToSQLController extends AbstractFormController<ChangeFeedToSQLForm> {
    @Autowired
    private BatchJobLauncher batchJobLauncher;

    @Autowired
    private ChangeFeedDispatcher changeFeedDispatcher;

    @Override
    @GetMapping(value = "/form")
    public ResponseEntity<ChangeFeedToSQLForm> getFormTemplate(@RequestParam Map<String, String> requestParams)
            throws JobExecutionException {
        // Trigger for template pre-filling via introspection
        final String table = requestParams.getOrDefault("table", null);

        ChangeFeedToSQLForm form = new ChangeFeedToSQLForm();
        form.setSubscriberId(UUID.randomUUID().toString());
        form.setTable(table);
        form.setPollTimeoutSeconds(templateProperties.getPollTimeoutSeconds());
        form.setChunkSize(templateProperties.getChunkSize());
        form.setConcurrency(templateProperties.getConcurrency());

        // Source DB
        form.setChangeFeedQuery(createSimpleWebhookChangeFeedStatement(
                form.getTable(), form.getSubscriberId()));
        form.setSourceUrl(requestParams
                .getOrDefault("sourceUrl", templateProperties.getSource().getUrl()));
        form.setSourceUsername(requestParams
                .getOrDefault("sourceUsername", templateProperties.getSource().getUsername()));
        form.setSourcePassword(requestParams
                .getOrDefault("sourcePassword", templateProperties.getSource().getPassword()));

        // Target DB
        form.setTargetUrl(
                requestParams.getOrDefault("targetUrl", templateProperties.getTarget().getUrl()));
        form.setTargetUsername(
                requestParams.getOrDefault("targetUsername", templateProperties.getTarget().getUsername()));
        form.setTargetPassword(
                requestParams.getOrDefault("targetPassword", templateProperties.getTarget().getPassword()));

        if (StringUtils.hasLength(table)) {
            final String sourceUrl = form.getSourceUrl();
            final String sourceUser = form.getSourceUsername();
            final String sourcePassword = form.getSourcePassword();

            Assert.hasLength(sourceUrl, "sourceUrl is required for auto-templating");

            try (ClosableDataSource dataSource = dataSourceFactory.apply(DataSourceProps.builder()
                    .withUrl(sourceUrl)
                    .withUsername(sourceUser)
                    .withPassword(sourcePassword)
                    .withName("cdc2sql-source")
                    .build())) {
                form.setCreateQuery(DatabaseInfo.showCreateTable(dataSource, table).orElse(
                        "(unable to introspect - template source '" + sourceUrl + "' is not CockroachDB)"));
                form.setInsertQuery(DatabaseInfo.createUpsertForTable(dataSource, table)
                        .orElse("(table '" + table + "' not found)"));
                form.setDeleteQuery(DatabaseInfo.createDeleteForTable(dataSource, table)
                        .orElse("(table '" + table + "' not found)"));
            }
            form.add(linkTo(methodOn(getClass())
                    .getFormTemplate(requestParams)).withSelfRel());
        } else {
            form.setCreateQuery("<create statement or DDL file>");
            form.setInsertQuery("<insert or upsert statement with named parameters>");
            form.setDeleteQuery("<delete statement with named parameters>");
            form.add(Link.of(fromCurrentContextPath()
                            .pathSegment("templates", "products-ddl.sql")
                            .toUriString())
                    .withRel(LinkRels.TEMPLATE_REL)
                    .withName("products-ddl.sql")
                    .withTitle("Sample products schema DDL (SQL)"));

            form.add(affordances(getClass()).toLink());
        }

        return ResponseEntity.ok(form);
    }

    @Override
    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<ChangeFeedModel> submitForm(@Valid @RequestBody ChangeFeedToSQLForm form)
            throws JobExecutionException {
        Subscriber subscriber = new Subscriber(form.getSubscriberId())
                .setCreationTimestamp(LocalDateTime.now())
                .setPollTimeout(Duration.ofSeconds(form.getPollTimeoutSeconds()))
                .setTableName(form.getTable());

        final ChangeFeedItemReader itemReader
                = new ChangeFeedItemReader(subscriber, changeFeedDispatcher);

        final ClosableDataSource targetDS = dataSourceFactory.apply(form.toTargetDataSourceProperties());

        final ItemWriter<Map<String, Object>> upsertItemWriter = NamedParameterItemWriter.builder()
                .setDataSource(targetDS)
                .setCreateQuery(form.getCreateQuery())
                .setUpdateQuery(form.getInsertQuery())
                .build();

        final ItemWriter<Map<String, Object>> deleteItemWriter = NamedParameterItemWriter.builder()
                .setDataSource(targetDS)
                .setUpdateQuery(form.getDeleteQuery())
                .setAssertUpdates(false)
                .build();

        final CompositeItemWriter itemWriter = new CompositeItemWriter(upsertItemWriter, deleteItemWriter);

        final String changeFeedStatement = form.getChangeFeedQuery();

        final BatchJobManifest batchJobManifest = BatchJobManifest.builder()
                .withRandomId()
                .withName("cdc2sql")
                .withChunkSize(form.getChunkSize())
                .withRestartExecutionId(form.getRestartExecutionId())
                .withFaultTolerance()
                .withConcurrency(form.getConcurrency())
                .withJobParameters(builder -> builder
                        .addString("createStatement", changeFeedStatement)
                        .addString("subscriberId", form.getSubscriberId())
                        .addString("table", form.getTable()))
                .build();

        logger.info("Creating cdc2sql job with poll timeout {}s (job expiry after first event)",
                form.getPollTimeoutSeconds());

        batchJobLauncher.submitJob(batchJobManifest, itemReader, itemWriter);

        if (StringUtils.hasLength(form.getChangeFeedQuery())) {
            logger.info("Create changefeed in source DB: {}", changeFeedStatement);
            try (ClosableDataSource sourceDS = dataSourceFactory.apply(form.toSourceDataSourceProperties())) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(sourceDS);
                jdbcTemplate.execute(changeFeedStatement);
            }
        }

        final ChangeFeedModel model = new ChangeFeedModel();
        model.setMessage("cdc2sql job accepted for async processing");
        model.setSimpleChangeFeedStatement(changeFeedStatement);
        model.setAdvancedChangeFeedStatement(
                createAdvancedWebhookChangeFeedStatement(form.getTable(), form.getSubscriberId()));
        model.add(linkTo(methodOn(ChangeFeedWebHookController.class)
                .onChangeEvent(form.getSubscriberId(), null))
                .withRel(LinkRels.CDC2SQL_SINK_REL)
                .withTitle("cdc2sql sink endpoint"));
        model.add(WebMvcLinkBuilder.linkTo(methodOn(JobController.class)
                        .getFutureJobExecution(batchJobManifest.getId()))
                .withRel(LinkRels.JOB_EXECUTION_REL)
                .withTitle("job execution resource"));

        return ResponseEntity.accepted().body(model);
    }

    @PostMapping(value = {"/bundle"})
    public ResponseEntity<CollectionModel<ChangeFeedModel>> submitFormBundle(
            @RequestBody CollectionModel<ChangeFeedToSQLForm> bundle,
            @RequestParam(value = "order", required = false) String order)
            throws JobExecutionException {
        Collection<ChangeFeedToSQLForm> forms = new ArrayList<>(bundle.getContent());
        List<ChangeFeedToSQLForm> orderedForms = new ArrayList<>();
        List<String> tablesInOrder
                = Arrays.stream(StringUtils.commaDelimitedListToStringArray(order)).toList();

        if (!tablesInOrder.isEmpty()) {
            tablesInOrder.forEach(table -> {
                ChangeFeedToSQLForm match
                        = forms.stream().filter(form -> table.equals(form.getTable())).findFirst()
                        .orElseThrow(() -> new JobConfigurationException("No such table exist: " + table));
                orderedForms.add(match);
            });
        } else {
            orderedForms.addAll(forms);
        }

        List<ChangeFeedModel> models = new ArrayList<>();

        for (ChangeFeedToSQLForm form : orderedForms) {
            logger.info("Submitting cdc2sql form for table: {}", form.getTable());
            ResponseEntity<ChangeFeedModel> responseEntity = submitForm(form);
            ChangeFeedModel responseBody = responseEntity.getBody();
            models.add(responseBody);
        }

        // FK topology order for tpc-c
        // warehouse,district,customer,history,"order",new_order,item,stock,order_line

        return ResponseEntity.accepted().body(CollectionModel.of(models));
    }

    private String createSimpleWebhookChangeFeedStatement(String table, String subscriberId) {
        String href = callbackHref(subscriberId);
        return "CREATE CHANGEFEED FOR TABLE " + table
                + " INTO 'webhook-" + href + "'"
                + " WITH updated;";
    }

    private String createAdvancedWebhookChangeFeedStatement(String table, String subscriberId) {
        String href = callbackHref(subscriberId);
        return "CREATE CHANGEFEED FOR TABLE " + table
                + " INTO 'webhook-" + href + "'"
                + " WITH updated, resolved='15s', "
                + " webhook_sink_config='{\"Flush\": {\"Messages\": 64, \"Frequency\": \"1s\"}, \"Retry\": {\"Max\": \"inf\"}}';";
    }

    private String callbackHref(String subscriberId) {
        return linkTo(methodOn(ChangeFeedWebHookController.class)
                .onChangeEvent(subscriberId, null))
                .toUriComponentsBuilder()
                .scheme("https")
                .host(AddressUtils.getLocalIP())
                .port(8443)
                .queryParam("insecure_tls_skip_verify", true)
                .build()
                .toUriString();
    }
}
