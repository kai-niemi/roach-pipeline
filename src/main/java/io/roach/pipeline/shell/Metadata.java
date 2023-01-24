package io.roach.pipeline.shell;

import java.util.function.Function;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import io.roach.pipeline.config.ClosableDataSource;
import io.roach.pipeline.config.TemplateProperties;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.util.graph.Graph;

@ShellComponent
@ShellCommandGroup(CommandGroups.PREVIEW)
public class Metadata {

    @Autowired
    private Console console;

    @Autowired
    private Function<DataSourceProps, ClosableDataSource> dataSourceFactory;

    @Autowired
    private TemplateProperties templateProperties;

    @ShellMethod(value = "List tables", key = {"tables"})
    public void listTables(
            @ShellOption(help = "source database JDBC url", defaultValue = "(source-template)") String url,
            @ShellOption(help = "source database JDBC user name", defaultValue = "(source-template)") String username,
            @ShellOption(help = "source database JDBC password", defaultValue = "(source-template)") String password
    ) {
        url = "(source-template)".equals(url) ? templateProperties.getSource().getUrl() : url;
        username = "(source-template)".equals(username) ? templateProperties.getSource().getUsername() : username;
        password = "(source-template)".equals(password) ? templateProperties.getSource().getPassword() : password;

        final DataSourceProps dataSourceProperties = DataSourceProps.builder()
                .withUrl(url)
                .withUsername(username)
                .withPassword(password)
                .withName("metadata")
                .build();

        final DataSource dataSource = dataSourceFactory.apply(dataSourceProperties);

        Graph<String, DatabaseInfo.ForeignKey> graph = new Graph<>();

        DatabaseInfo.listTables(dataSource, "public").forEach(name -> {

            console.printf(AnsiColor.BRIGHT_WHITE, "Table: %s\n", name);
            console.printf(AnsiColor.BRIGHT_YELLOW, "-- Columns --\n");

            DatabaseInfo.listColumns(dataSource, name).forEach((s, column) -> {
                console.printf(AnsiColor.BRIGHT_GREEN, "%s\n", s);
            });

            console.printf(AnsiColor.BRIGHT_YELLOW, "-- Foreign Keys --\n");

            graph.addNode(name);

            DatabaseInfo.listForeignKeys(dataSource, name).forEach(foreignKey -> {
                graph.addNode(foreignKey.getPkTableName());
                graph.addEdge(name, foreignKey.getPkTableName(), foreignKey);

                console.printf(AnsiColor.BRIGHT_GREEN, "%s\n", foreignKey);
            });
        });

        console.printf(AnsiColor.BRIGHT_YELLOW, "-- Dependency graph--\n");
        console.printf(AnsiColor.BRIGHT_WHITE, "%s\n", graph.toString());
        console.printf(AnsiColor.BRIGHT_YELLOW, "FK topological order:\n");

        try {
            console.printf(AnsiColor.BRIGHT_GREEN, "%s\n", graph.topologicalSort());
        } catch (IllegalStateException e) { // Not a DAG
            console.printf(AnsiColor.BRIGHT_RED, e.toString());
        }
    }
}
