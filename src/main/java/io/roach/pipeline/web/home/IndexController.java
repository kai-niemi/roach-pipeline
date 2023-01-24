package io.roach.pipeline.web.home;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ResourceBanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.roach.pipeline.Application;
import io.roach.pipeline.config.ApplicationProfiles;
import io.roach.pipeline.config.ClosableDataSource;
import io.roach.pipeline.config.TemplateProperties;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.web.LinkRels;
import io.roach.pipeline.web.MessageModel;
import io.roach.pipeline.web.admin.JobController;
import io.roach.pipeline.web.cdc.ChangeFeedToSQLController;
import io.roach.pipeline.web.cdc.KafkaToSQLController;
import io.roach.pipeline.web.csv.FlatToCSVController;
import io.roach.pipeline.web.csv.FlatToSQLController;
import io.roach.pipeline.web.sql.SQLtoCSVController;
import io.roach.pipeline.web.sql.SQLtoSQLController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(path = "/")
@ApplicationProfiles.Online
public class IndexController {
    @Autowired
    private Environment environment;

    @Autowired
    protected TemplateProperties templateProperties;

    @Autowired
    protected Function<DataSourceProps, ClosableDataSource> dataSourceFactory;

    @GetMapping
    public ResponseEntity<MessageModel> index() throws MissingRequestValueException, JobExecutionException {
        MessageModel index = MessageModel.from(readMessageOfTheDay());
        index.setNotice("For an interactive API browser follow the " + LinkRels.HAL_EXPLORER_REL + " link URI");

        index.add(linkTo(methodOn(IndexController.class)
                .index())
                .withSelfRel());

        index.add(linkTo(methodOn(getClass())
                .getTemplateTables(Collections.emptyMap()))
                .withRel(LinkRels.TABLES_REL)
                .withTitle("Template source database tables"));

        index.add(linkTo(methodOn(JobController.class)
                .listJobs())
                .withRel(LinkRels.JOBS_REL)
                .withTitle("Job execution status and metadata"));

        index.add(Link.of(
                        ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .pathSegment("actuator")
                                .buildAndExpand()
                                .toUriString()
                ).withRel(LinkRels.ACTUATOR_REL)
                .withTitle("Spring boot actuators for observability"));

        String rootUri =
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .buildAndExpand()
                        .toUriString();
        index.add(Link.of(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .pathSegment("browser/index.html")
                        .fragment("theme=Cosmo&uri=" + rootUri)
                        .buildAndExpand()
                        .toUriString())
                .withRel(LinkRels.HAL_EXPLORER_REL)
                .withTitle("API browser")
                .withType(MediaType.TEXT_HTML_VALUE)
        );

        index.add(linkTo(methodOn(FlatToCSVController.class)
                .getFormTemplate(Collections.emptyMap()))
                .withRel(LinkRels.FLAT2CSV_REL)
                .withTitle("Get a form template for flat-file to CSV jobs"));

        index.add(linkTo(methodOn(FlatToSQLController.class)
                .getFormTemplate(Collections.emptyMap()))
                .withRel(LinkRels.FLAT2SQL_REL)
                .withTitle("Get a form template for flat-file to SQL jobs"));
        index.add(linkTo(methodOn(FlatToSQLController.class)
                .getFormTemplates(Collections.emptyMap()))
                .withRel(LinkRels.FLAT2SQL_REL + LinkRels.BUNDLE_SUFFIX)
                .withTitle("Get a Flat to SQL form template bundle with all user tables"));
        index.add(linkTo(methodOn(FlatToSQLController.class)
                .getFormTemplatesBundle(Collections.emptyMap(), null))
                .withRel(LinkRels.FLAT2SQL_REL + LinkRels.ZIP_BUNDLE_SUFFIX)
                .withTitle("Get a Flat to SQL form template zip bundle with all user tables"));

        index.add(linkTo(methodOn(SQLtoCSVController.class)
                .getFormTemplate(Collections.emptyMap()))
                .withRel(LinkRels.SQL2CSV_REL)
                .withTitle("Get a form template for SQL to CSV jobs"));
        index.add(linkTo(methodOn(SQLtoCSVController.class)
                .getFormTemplates(Collections.emptyMap()))
                .withRel(LinkRels.SQL2CSV_REL + LinkRels.BUNDLE_SUFFIX)
                .withTitle("Get a SQL to CSV form template bundle with all user tables"));
        index.add(linkTo(methodOn(SQLtoCSVController.class)
                .getFormTemplatesBundle(Collections.emptyMap(), null))
                .withRel(LinkRels.SQL2CSV_REL + LinkRels.ZIP_BUNDLE_SUFFIX)
                .withTitle("Get a SQL to CSV form template zip bundle with all user tables"));

        index.add(linkTo(methodOn(SQLtoSQLController.class)
                .getFormTemplate(Collections.emptyMap()))
                .withRel(LinkRels.SQL2SQL_REL)
                .withTitle("Get a form template for SQL to SQL jobs")
        );
        index.add(linkTo(methodOn(SQLtoSQLController.class)
                .getFormTemplates(Collections.emptyMap()))
                .withRel(LinkRels.SQL2SQL_REL + LinkRels.BUNDLE_SUFFIX)
                .withTitle("Get a SQL to SQL form template bundle with all user tables"));
        index.add(linkTo(methodOn(SQLtoSQLController.class)
                .submitFormTemplates(null, null))
                .withRel(LinkRels.SQL2SQL_REL + LinkRels.BUNDLE_SUFFIX)
                .withTitle("Submit a SQL to SQL form template bundle for job scheduling"));
        index.add(linkTo(methodOn(SQLtoSQLController.class)
                .getFormTemplatesBundle(Collections.emptyMap(), null))
                .withRel(LinkRels.SQL2SQL_REL + LinkRels.ZIP_BUNDLE_SUFFIX)
                .withTitle("Get a SQL to SQL form template zip bundle with all user tables"));

        index.add(linkTo(methodOn(ChangeFeedToSQLController.class)
                .getFormTemplate(Collections.emptyMap()))
                .withRel(LinkRels.CDC2SQL_REL)
                .withTitle("Get a form template for CDC to SQL jobs"));
        index.add(linkTo(methodOn(ChangeFeedToSQLController.class)
                .getFormTemplates(Collections.emptyMap()))
                .withRel(LinkRels.CDC2SQL_REL + LinkRels.BUNDLE_SUFFIX)
                .withTitle("Get a CDC to SQL form template bundle with all user tables"));
        index.add(linkTo(methodOn(ChangeFeedToSQLController.class)
                .submitFormTemplates(null, null))
                .withRel(LinkRels.CDC2SQL_REL + LinkRels.BUNDLE_SUFFIX)
                .withTitle("Submit a CDC to SQL form template bundle for job scheduling"));
        index.add(linkTo(methodOn(ChangeFeedToSQLController.class)
                .getFormTemplatesBundle(Collections.emptyMap(), null))
                .withRel(LinkRels.CDC2SQL_REL + LinkRels.ZIP_BUNDLE_SUFFIX)
                .withTitle("Get a CDC to SQL form template zip bundle with all user tables"));

        index.add(linkTo(methodOn(KafkaToSQLController.class)
                .getFormTemplate(Collections.emptyMap()))
                .withRel(LinkRels.KAFKA2SQL_REL)
                .withTitle("Get a form template for Kafka to SQL jobs"));
        index.add(linkTo(methodOn(KafkaToSQLController.class)
                .getFormTemplates(Collections.emptyMap()))
                .withRel(LinkRels.KAFKA2SQL_REL + LinkRels.BUNDLE_SUFFIX)
                .withTitle("Get a Kafka to SQL form template bundle with all user tables"));
        index.add(linkTo(methodOn(KafkaToSQLController.class)
                .submitFormTemplates(null, null))
                .withRel(LinkRels.KAFKA2SQL_REL + LinkRels.BUNDLE_SUFFIX)
                .withTitle("Submit a Kafka to SQL form template bundle for job scheduling"));
        index.add(linkTo(methodOn(KafkaToSQLController.class)
                .getFormTemplatesBundle(Collections.emptyMap(), null))
                .withRel(LinkRels.KAFKA2SQL_REL + LinkRels.ZIP_BUNDLE_SUFFIX)
                .withTitle("Get a Kafka to SQL form template zip bundle with all user tables"));

        return new ResponseEntity<>(index, HttpStatus.OK);
    }

    @PostMapping("/inform")
    public ResponseEntity<?> informPage(Model model) {
        return ResponseEntity.ok().build();
    }

    private String readMessageOfTheDay() {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bas);
        new ResourceBanner(new ClassPathResource("motd.txt"))
                .printBanner(environment, Application.class, ps);
        ps.flush();
        return bas.toString().replace("\r\n", "").trim();
    }

    @GetMapping(value = {"/tables"})
    public ResponseEntity<CollectionModel<MessageModel>> getTemplateTables(
            @RequestParam Map<String, String> requestParams) {
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

            List<MessageModel> models = new ArrayList<>();

            DatabaseInfo.listTables(dataSource, schema).forEach(table -> {
                try {
                    MessageModel model = MessageModel.from(table);
                    model.add(linkTo(methodOn(KafkaToSQLController.class)
                            .getFormTemplate(Collections.singletonMap("table", table)))
                            .withRel(LinkRels.KAFKA2SQL_REL));
                    model.add(linkTo(methodOn(ChangeFeedToSQLController.class)
                            .getFormTemplate(Collections.singletonMap("table", table)))
                            .withRel(LinkRels.CDC2SQL_REL));
                    model.add(linkTo(methodOn(SQLtoSQLController.class)
                            .getFormTemplate(Collections.singletonMap("table", table)))
                            .withRel(LinkRels.SQL2SQL_REL));
                    model.add(linkTo(methodOn(SQLtoCSVController.class)
                            .getFormTemplate(Collections.singletonMap("table", table)))
                            .withRel(LinkRels.SQL2CSV_REL));
                    models.add(model);
                } catch (JobExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

            return ResponseEntity.ok()
                    .body(CollectionModel.of(models));
        }
    }

}
