package io.roach.pipeline.shell;

import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.ReflectionUtils;

@ShellComponent
@ShellCommandGroup(CommandGroups.ADMIN)
public class DatabaseVersion {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private Console console;

    @ShellMethod(value = "Print database metadata", key = {"db-metadata", "m"})
    @ShellMethodAvailability("connectedCheck")
    public void metadata(
            @ShellOption(help = "Include all no-arg database metadata methods", defaultValue = "false") boolean all) {
        JdbcTemplate template = new JdbcTemplate(dataSource);

        template.execute((ConnectionCallback<Object>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();

            Map<String, Object> properties = new TreeMap<>();
            if (all) {
                ReflectionUtils.doWithMethods(java.sql.DatabaseMetaData.class, method -> {
                    if (method.getParameterCount() == 0) {
                        try {
                            Object rv = method.invoke(metaData);
                            properties.put(method.getName(), rv);
                        } catch (InvocationTargetException e) {
                            console.warning(e.getTargetException().getMessage());
                        }
                    }
                });
            } else {
                properties.put("databaseProductName", metaData.getDatabaseProductName());
                properties.put("databaseMajorVersion", metaData.getDatabaseMajorVersion());
                properties.put("databaseMinorVersion", metaData.getDatabaseMinorVersion());
                properties.put("databaseProductVersion", metaData.getDatabaseProductVersion());
                properties.put("driverMajorVersion", metaData.getDriverMajorVersion());
                properties.put("driverMinorVersion", metaData.getDriverMinorVersion());
                properties.put("driverName", metaData.getDriverName());
                properties.put("driverVersion", metaData.getDriverVersion());
                properties.put("maxConnections", metaData.getMaxConnections());
                properties.put("defaultTransactionIsolation", metaData.getDefaultTransactionIsolation());
                properties.put("transactionIsolation", connection.getTransactionIsolation());
            }

            properties.forEach((k, v) -> {
                console.printf(AnsiColor.BRIGHT_YELLOW, "%s = ", k);
                console.printf(AnsiColor.BRIGHT_CYAN, "%s\n", v);
            });
            return null;
        });
    }

    @ShellMethod(value = "Print database version", key = {"db-version", "v"})
    @ShellMethodAvailability("connectedCheck")
    public void version() {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        console.information(template.queryForObject("select version()", String.class));
    }
}