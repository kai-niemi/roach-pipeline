package io.roach.pipeline.shell.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

import io.roach.pipeline.config.ClosableDataSource;
import io.roach.pipeline.config.TemplateProperties;
import io.roach.pipeline.expression.MapRegistry;
import io.roach.pipeline.expression.RuleExpression;
import io.roach.pipeline.item.flatfile.FlatFileResourceWriterBuilder;
import io.roach.pipeline.item.flatfile.schema.FlatFileSchema;
import io.roach.pipeline.item.flatfile.schema.FlatFileSchemaUtils;
import io.roach.pipeline.shell.CommandGroups;
import io.roach.pipeline.shell.Console;
import io.roach.pipeline.shell.support.DatabaseInfo;
import io.roach.pipeline.util.ConcurrencyUtils;
import io.roach.pipeline.util.DataSourceProps;
import io.roach.pipeline.util.EnumPattern;
import io.roach.pipeline.util.RandomData;

@ShellComponent
@ShellCommandGroup(CommandGroups.PREVIEW)
public class CSV {
    private static int spinner = 0;

    private static void tick(String prefix) {
        System.out.printf(Locale.US, "\r%25s (%s)", prefix, "|/-\\".toCharArray()[spinner++ % 4]);
    }

    @Autowired
    private Console console;

    @Autowired
    private Function<DataSourceProps, ClosableDataSource> dataSourceFactory;

    @Autowired
    private TemplateProperties templateProperties;

    // csv --table users --url jdbc:postgresql://192.168.1.99:26257/tpcc --delimiter ;
    // csv --table users --url jdbc:postgresql://localhost:26257/tpcc --delimiter ;
    @ShellMethod(value = "Generate CSV file from table schema comment expressions")
    public void csv(
            @ShellOption(help = "source database JDBC url", defaultValue = "(source-template)")
            String url,
            @ShellOption(help = "source database JDBC user name", defaultValue = "(source-template)") String username,
            @ShellOption(help = "source database JDBC password", defaultValue = "(source-template)") String password,
            @ShellOption(help = "source table name") String table,
            @ShellOption(help = "comma delimited list of column names or '*' to include all based on column policy", defaultValue = "*")
            String columnNames,
            @ShellOption(help = "column exclusion policy as a comma separated list of:"
                    + "\nINCLUDE_ALL = include all columns"
                    + "\nSKIP_EMPTY_COMMENTS = skip columns with empty comment"
                    + "\nSKIP_DEFAULT = skip columns with column default value",
                    defaultValue = "SKIP_EMPTY_COMMENT,SKIP_DEFAULT")
            String columnPolicy,
            @ShellOption(help = "number of files", defaultValue = "1") int
                    numFiles,
            @ShellOption(help = "number of rows to generate", defaultValue = "100") int rowCount,
            @ShellOption(help = "field delimiter (note: avoid comma with json columns)", defaultValue = ",")
            String delimiter
    ) {
        url = "(source-template)".equals(url) ? templateProperties.getSource().getUrl() : url;
        username = "(source-template)".equals(username) ? templateProperties.getSource().getUsername() : username;
        password = "(source-template)".equals(password) ? templateProperties.getSource().getPassword() : password;

        List<String> columnsIncluded = "*".equals(columnNames)
                ? Collections.emptyList()
                : Arrays.asList(columnNames.split(","));

        EnumSet<ColumnPolicy> columnPolicies = EnumPattern.parse(columnPolicy, ColumnPolicy.class);

        final DataSourceProps dataSourceProps = DataSourceProps.builder()
                .withUrl(url)
                .withUsername(username)
                .withPassword(password)
                .withName("pipeline")
                .build();

        final DataSource dataSource = dataSourceFactory.apply(dataSourceProps);

        final Map<String, DatabaseInfo.Column> metaData = DatabaseInfo.listColumns(dataSource, table);

        final FlatFileSchema flatFileSchema = FlatFileSchemaUtils.generateSchema(
                dataSource, table, delimiter, field -> {
                    if (columnsIncluded.isEmpty()) {
                        boolean skipEmptyComments = columnPolicies.contains(ColumnPolicy.SKIP_EMPTY_COMMENT);
                        if (!StringUtils.hasLength(field.getComment()) && skipEmptyComments) {
                            field.setIgnored(true);
                        }

                        boolean skipDefault = columnPolicies.contains(ColumnPolicy.SKIP_DEFAULT);
                        String columnDef = metaData.get(field.getName()).getAttributes().getOrDefault("COLUMN_DEF", "");
                        if (StringUtils.hasLength(columnDef) && skipDefault) {
                            field.setIgnored(true);
                        }
                    } else {
                        field.setIgnored(!columnsIncluded.contains(field.getName()));
                    }
                    return !field.isIgnored();
                });

        if (flatFileSchema.fieldNames().isEmpty()) {
            console.information("No fields found for schema - cancelling");
            return;
        }

        try {
            Path p = Paths.get(table + "-schema.json");
            FlatFileSchemaUtils.writeToFile(flatFileSchema, p);
            console.printf(AnsiColor.BRIGHT_CYAN, "Created schema file '%s'\n", p);
        } catch (IOException e) {
            console.error(e.toString());
        }

        final List<Runnable> tasks = new ArrayList<>();
        final int rowsPerWorker = rowCount / numFiles;

        IntStream.rangeClosed(1, numFiles).forEach(idx -> {
            tasks.add(() -> {
                final MapRegistry registry = FunctionInventory.buildFunctions(dataSource);
                final AtomicInteger rowNumber = new AtomicInteger();
                registry.addFunction("rowNumber", args -> rowNumber.get());

                final Path outputPath = Paths.get(table + "-" + idx + ".csv");
                console.printf(AnsiColor.BRIGHT_CYAN, "Creating file %s with %d rows\n", outputPath, rowsPerWorker);

                final FlatFileItemWriter<Map<String, Object>> itemWriter = FlatFileResourceWriterBuilder.instance()
                        .setFieldNames(flatFileSchema.allFieldNames())
                        .setOutputResource(new FileSystemResource(outputPath))
                        .setDelimiter(delimiter)
                        .build();

                try {
                    itemWriter.open(new ExecutionContext());

                    IntStream.rangeClosed(1, rowsPerWorker).forEach(value -> {
                        rowNumber.set(value);

                        Map<String, Object> values = new LinkedHashMap<>();

                        flatFileSchema.getIncludedFields().forEach(field -> {
                            String comment = field.getComment();
                            if (StringUtils.hasLength(comment)) {
                                values.put(field.getName(),
                                        RuleExpression.evaluate(comment, String.class, registry));
                            } else {
                                values.put(field.getName(),
                                        String.valueOf(RandomData.randomValue(metaData.get(field.getName()))));
                            }
                        });

                        if (value % 1000 == 0) {
                            tick(outputPath + " " + Math.round((0f + value) / rowsPerWorker * 100.0) + "%");
                        }

                        try {
                            itemWriter.write(Chunk.of(values));
                        } catch (Exception e) {
                            console.error(e.toString());
                        }
                    });
                } finally {
                    itemWriter.close();
                    try {
                        console.printf(AnsiColor.BRIGHT_CYAN, "Done with %s (%d rows %,d bytes)\n", outputPath, rowCount,
                                Files.size(outputPath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });

        console.printf(AnsiColor.BRIGHT_CYAN, "Queued %d tasks\n", tasks.size());
        ConcurrencyUtils.runConcurrentlyAndWait(tasks);
        console.information("All tasks completed");
    }

    @ShellMethod(value = "List CSV column functions", key = {"functions"})
    public void listFunctions() {
        final MapRegistry registry = FunctionInventory.buildFunctions(null);
        console.information("-- Functions --");
        registry.functionNames().forEach(fn -> {
            console.printf(AnsiColor.BRIGHT_CYAN, "\t%s\n", fn);
        });
    }

    @ShellMethod(value = "Evaluate column expression", key = {"eval"})
    public void evaluateFunction(
            @ShellOption(help = "rule expression", defaultValue = "selectOne('select gen_random_uuid()')")
            String expression,
            @ShellOption(help = "source database JDBC url", defaultValue = "(source-template)")
            String url,
            @ShellOption(help = "source database JDBC user name", defaultValue = "(source-template)") String username,
            @ShellOption(help = "source database JDBC password", defaultValue = "(source-template)") String password) {
        url = "(source-template)".equals(url) ? templateProperties.getSource().getUrl() : url;
        username = "(source-template)".equals(username) ? templateProperties.getSource().getUsername() : username;
        password = "(source-template)".equals(password) ? templateProperties.getSource().getPassword() : password;

        final DataSource dataSource = dataSourceFactory.apply(DataSourceProps.builder()
                .withUrl(url)
                .withUsername(username)
                .withPassword(password)
                .withName("pipeline")
                .build());

        final MapRegistry registry = FunctionInventory.buildFunctions(dataSource);
        Object result = RuleExpression.evaluate(expression, Object.class, registry);
        console.information("Expression: " + expression);
        console.information("Result: " + result);
        console.information("Result type: " + result.getClass().getName());
    }

/*
    @ShellMethod(value = "List tables", key = {"tables"})
    public void listTables(
            @ShellOption(help = "source database JDBC url",
                    defaultValue = "jdbc:postgresql://localhost:26257/test") String url,
            @ShellOption(help = "source database JDBC user name", defaultValue = "root") String username,
            @ShellOption(help = "source database JDBC password", defaultValue = "") String password
    ) {
        final DataSourceProps dataSourceProperties = DataSourceProps.builder()
                .withUrl(url)
                .withUsername(username)
                .withPassword(password)
                .withName("pipeline")
                .build();

        final DataSource dataSource = dataSourceFactory.apply(dataSourceProperties);

        DatabaseInfo.listTables(dataSource, "public").forEach(name -> {
            console.information("Table: %s", name);
            console.information("-- Columns --");

            DatabaseInfo.listColumns(dataSource, name).forEach((s, column) -> {
                console.information("%s", s);
            });

            console.information("-- Foreign Keys --");
            DatabaseInfo.listForeignKeys(dataSource, name).forEach(foreignKey -> {
                console.information("%s", foreignKey);
            });
        });
    }
*/
}
