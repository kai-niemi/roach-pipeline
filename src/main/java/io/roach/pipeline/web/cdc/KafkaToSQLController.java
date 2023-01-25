package io.roach.pipeline.web.cdc;

import java.time.Duration;
import java.util.*;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.kafka.KafkaItemReader;
import org.springframework.batch.item.kafka.builder.KafkaItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.JsonDeserializer;
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
import io.roach.pipeline.item.changefeed.CompositeItemWriter;
import io.roach.pipeline.item.changefeed.KafkaChangeEvent;
import io.roach.pipeline.item.jdbc.NamedParameterItemWriter;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.AddressUtils;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.web.AbstractFormController;
import io.roach.pipeline.web.LinkRels;
import io.roach.pipeline.web.admin.JobController;
import jakarta.validation.Valid;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@RestController
@RequestMapping("/kafka2sql")
@ApplicationProfiles.Online
public class KafkaToSQLController extends AbstractFormController<KafkaToSQLForm> {
    @Autowired
    private BatchJobLauncher batchJobLauncher;

    @Override
    @GetMapping(value = "/form")
    public ResponseEntity<KafkaToSQLForm> getFormTemplate(@RequestParam Map<String, String> requestParams)
            throws JobExecutionException {
        final String table = requestParams.getOrDefault("table", null);

        KafkaToSQLForm form = new KafkaToSQLForm();
        form.setConcurrency(1);
        form.setTable(table);
        form.setBootstrapServers(requestParams.getOrDefault("bootstrapServers",
                templateProperties.getKafka().getBootstrapServers()));
        form.setGroupId(requestParams.getOrDefault("groupId",
                templateProperties.getKafka().getGroupId()));
        form.setPollTimeoutSeconds(
                templateProperties.getPollTimeoutSeconds());
        form.setChunkSize(
                templateProperties.getChunkSize());

        // Target DB
        form.setTargetUrl(requestParams.getOrDefault("targetUrl", templateProperties.getTarget().getUrl()));
        form.setTargetUsername(
                requestParams.getOrDefault("targetUsername", templateProperties.getTarget().getUsername()));
        form.setTargetPassword(
                requestParams.getOrDefault("targetPassword", templateProperties.getTarget().getPassword()));

        if (StringUtils.hasLength(table)) {
            form.setName(table + "-reader");
            form.setTopic(table);

            final String sourceUrl = requestParams
                    .getOrDefault("sourceUrl", templateProperties.getSource().getUrl());
            final String sourceUser = requestParams
                    .getOrDefault("sourceUsername", templateProperties.getSource().getUsername());
            final String sourcePassword = requestParams
                    .getOrDefault("sourcePassword", templateProperties.getSource().getPassword());

            Assert.hasLength(sourceUrl, "sourceUrl is required for auto-templating");

            try (ClosableDataSource dataSource = dataSourceFactory.apply(DataSourceProps.builder()
                    .withUrl(sourceUrl)
                    .withUsername(sourceUser)
                    .withPassword(sourcePassword)
                    .withName("kafka2sql-source")
                    .build())) {

                form.setInsertQuery(DatabaseInfo.createUpsertForTable(dataSource, table)
                        .orElse("(table '" + table + "' not found)"));
                form.setCreateQuery(DatabaseInfo.showCreateTable(dataSource, table).orElse(
                        "(unable to introspect - template source '" + sourceUrl + "' is not CockroachDB)"));
                form.setDeleteQuery(DatabaseInfo.createDeleteForTable(dataSource, table)
                        .orElse("(table '" + table + "' not found)"));
            }
            form.add(linkTo(methodOn(getClass())
                    .getFormTemplate(requestParams)).withSelfRel());
        } else {
            form.setName("products-reader");
            form.setTopic("<table name>");
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
    @PostMapping
    public ResponseEntity<ChangeFeedModel> submitForm(@Valid @RequestBody KafkaToSQLForm form)
            throws JobExecutionException {
        Properties consumerProperties = new Properties();
        consumerProperties.setProperty(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, form.getBootstrapServers());
        consumerProperties.setProperty(
                ConsumerConfig.CLIENT_DNS_LOOKUP_CONFIG, "use_all_dns_ips");
        consumerProperties.setProperty(
                ConsumerConfig.GROUP_ID_CONFIG, form.getGroupId());
        consumerProperties.setProperty(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.setProperty(
                JsonDeserializer.VALUE_DEFAULT_TYPE, KafkaChangeEvent.class.getName());
        consumerProperties.setProperty(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        consumerProperties.setProperty(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class.getName());

        // Empty map denotes using stored (if any) consumer group offsets
        final Map<TopicPartition, Long> partitionOffsets = new HashMap<>();

        KafkaItemReader<String, KafkaChangeEvent> itemReader = new KafkaItemReaderBuilder<String, KafkaChangeEvent>()
                .name(form.getName())
                .topic(form.getTopic())
                .partitions(0)
                .pollTimeout(Duration.ofSeconds(form.getPollTimeoutSeconds()))
                .saveState(form.isSaveOffsets())
                .partitionOffsets(partitionOffsets)
                .consumerProperties(consumerProperties)
                .build();

        final DataSourceProps targetDBProperties = form.toTargetDataSourceProperties();

        final ClosableDataSource targetDS = dataSourceFactory.apply(targetDBProperties);

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

        final String changeFeedStatement = kafkaChangeFeedStatement(form.getTable(), form.getBootstrapServers());

        final BatchJobManifest batchJobManifest = BatchJobManifest.builder()
                .withRandomId()
                .withName("kafka2sql")
                .withJobParameters(builder -> builder
                        .addString("createStatement", changeFeedStatement)
                        .addString("table", form.getTable()))
                .withChunkSize(form.getChunkSize())
                .withRestartExecutionId(form.getRestartExecutionId())
                .withFaultTolerance()
                .withConcurrency(form.getConcurrency())
                .build();

        batchJobLauncher.submitJob(batchJobManifest, itemReader, itemWriter);

        final ChangeFeedModel model = new ChangeFeedModel();
        model.setMessage("kafka2sql job accepted for async processing");
        model.setSimpleChangeFeedStatement(changeFeedStatement);
        model.add(WebMvcLinkBuilder.linkTo(methodOn(JobController.class)
                        .getFutureJobExecution(batchJobManifest.getId()))
                .withRel(LinkRels.JOB_EXECUTION_REL)
                .withTitle("job execution resource"));

        return ResponseEntity.accepted().body(model);
    }

    @PostMapping(value = {"/forms"})
    public ResponseEntity<CollectionModel<ChangeFeedModel>> submitFormTemplateBundle(
            @RequestBody CollectionModel<KafkaToSQLForm> bundle)
            throws JobExecutionException {
        List<ChangeFeedModel> models = new ArrayList<>();

        for (KafkaToSQLForm form : bundle) {
            logger.info("Submitting form for table: {}", form.getTable());
            models.add(submitForm(form).getBody());
        }

        return ResponseEntity.accepted().body(CollectionModel.of(models));
    }


    private String kafkaChangeFeedStatement(String table, String bootstrapServers) {
        Set<String> servers = StringUtils.commaDelimitedListToSet(bootstrapServers);
        if (servers.size() > 0) {
            return "CREATE CHANGEFEED FOR TABLE " + table
                    + " INTO 'kafka://" + servers.iterator().next() + "' WITH updated,resolved = '15s';";
        } else {
            String host = AddressUtils.getLocalIP() + ":9093";
            return "CREATE CHANGEFEED FOR TABLE " + table
                    + " INTO 'kafka://" + host + "' WITH updated,resolved = '15s';";
        }
    }
}
