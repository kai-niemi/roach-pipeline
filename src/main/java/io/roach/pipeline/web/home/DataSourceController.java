package io.roach.pipeline.web.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.roach.pipeline.config.ApplicationProfiles;
import io.roach.pipeline.config.ClosableDataSource;
import io.roach.pipeline.config.TemplateProperties;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.util.graph.Graph;
import io.roach.pipeline.web.LinkRels;
import io.roach.pipeline.web.TableModel;
import io.roach.pipeline.web.cdc.ChangeFeedToSQLController;
import io.roach.pipeline.web.cdc.KafkaToSQLController;
import io.roach.pipeline.web.sql.SQLtoCSVController;
import io.roach.pipeline.web.sql.SQLtoSQLController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(path = "/datasource")
@ApplicationProfiles.Online
public class DataSourceController {
    @Autowired
    protected TemplateProperties templateProperties;

    @Autowired
    protected Function<DataSourceProps, ClosableDataSource> dataSourceFactory;

    @GetMapping(value = {"/source-tables"})
    public ResponseEntity<DataSourceTableModel> getSourceTables(
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

            Map<String, TableModel> tableModels = new HashMap<>();

            Graph<String, DatabaseInfo.ForeignKey> graph = new Graph<>();

            DatabaseInfo.listTables(dataSource, schema).forEach(table -> {
                graph.addNode(table);

                DatabaseInfo.listForeignKeys(dataSource, table).forEach(foreignKey -> {
                    graph.addNode(foreignKey.getPkTableName());
                    graph.addEdge(table, foreignKey.getPkTableName(), foreignKey);
                });

                try {
                    TableModel model = TableModel.of(table);
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
                    tableModels.put(table, model);
                } catch (JobExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

            DataSourceTableModel<TableModel> model;

            try {
                List<TableModel> sortedTableModels = new ArrayList<>();
                graph.topologicalSort(true).forEach(table -> sortedTableModels.add(tableModels.get(table)));

                model = new DataSourceTableModel<>(sortedTableModels, graph.topologicalSort(true));
                model.setUrl(sourceUrl);
            } catch (IllegalStateException e) {
                model = new DataSourceTableModel<>(tableModels.values(), Collections.emptyList());
                model.setUrl(sourceUrl);
                model.setMessage(e.getMessage());
            }

            return ResponseEntity.ok().body(model);
        }
    }

}
