package io.roach.pipeline.shell.support;

import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.StringUtils;

public abstract class DatabaseInfo {
    public static class Column {
        String name;

        Map<String, String> attributes = new TreeMap<>();

        public Column(String name, ResultSet columns) throws SQLException {
            this.name = name;
            ResultSetMetaData resultSetMetaData = columns.getMetaData();
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                attributes.put(resultSetMetaData.getColumnName(i), columns.getString(i));
            }
        }

        public String getName() {
            return name;
        }

        public String getAttribute(String name) {
            return attributes.getOrDefault(name, "???");
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }
    }

    public static class PrimaryKey {
        String tableName;

        String columnName;

        String keySeq;

        String pkName;

        public PrimaryKey(ResultSet resultSet) throws SQLException {
            this.tableName = resultSet.getString("TABLE_NAME");
            this.columnName = resultSet.getString("COLUMN_NAME");
            this.keySeq = resultSet.getString("KEY_SEQ");
            this.pkName = resultSet.getString("PK_NAME");
        }

        public String getTableName() {
            return tableName;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getKeySeq() {
            return keySeq;
        }

        public String getPkName() {
            return pkName;
        }

        @Override
        public String toString() {
            return "PrimaryKey{" +
                    "tableName='" + tableName + '\'' +
                    ", columnName='" + columnName + '\'' +
                    ", keySeq='" + keySeq + '\'' +
                    ", pkName='" + pkName + '\'' +
                    '}';
        }
    }

    public static class ForeignKey {
        String pkTableName;

        String fkTableName;

        String pkColumnName;

        String fkColumnName;

        public ForeignKey(ResultSet foreignKeys) throws SQLException {
            this.pkTableName = foreignKeys.getString("PKTABLE_NAME");
            this.fkTableName = foreignKeys.getString("FKTABLE_NAME");
            this.pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
            this.fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
        }

        public String getPkTableName() {
            return pkTableName;
        }

        public String getFkTableName() {
            return fkTableName;
        }

        public String getPkColumnName() {
            return pkColumnName;
        }

        public String getFkColumnName() {
            return fkColumnName;
        }

        @Override
        public String toString() {
            return "ForeignKey{" +
                    "pkTableName='" + pkTableName + '\'' +
                    ", fkTableName='" + fkTableName + '\'' +
                    ", pkColumnName='" + pkColumnName + '\'' +
                    ", fkColumnName='" + fkColumnName + '\'' +
                    '}';
        }
    }

    private DatabaseInfo() {
    }

    public static String databaseVersion(DataSource dataSource) {
        try {
            return new JdbcTemplate(dataSource).queryForObject("select version()", String.class);
        } catch (DataAccessException e) {
            return "unknown";
        }
    }

    public static boolean isCockroachDB(DataSource dataSource) {
        return databaseVersion(dataSource).contains("CockroachDB");
    }

    public static Optional<String> createImportIntoForTable(DataSource dataSource, String table, URI formUri) {
        List<String> columnNames = new ArrayList<>();
        List<String> columnParams = new ArrayList<>();

        DatabaseInfo.listColumns(dataSource, table).forEach((name, column) -> {
            columnNames.add(column.getName());
            columnParams.add(":" + column.getName());
        });

        if (columnNames.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of("IMPORT INTO "
                + table
                + "("
                + StringUtils.collectionToCommaDelimitedString(columnNames)
                + ") CSV DATA ("
                + formUri.toASCIIString()
                + ")");

    }

    public static Optional<String> createUpsertForTable(DataSource dataSource, String table) {
        List<String> columnNames = new ArrayList<>();
        List<String> columnParams = new ArrayList<>();

        DatabaseInfo.listColumns(dataSource, table).forEach((name, column) -> {
            columnNames.add(column.getName());
            columnParams.add(":" + column.getName());
        });

        if (columnNames.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of("UPSERT INTO "
                + table
                + "("
                + StringUtils.collectionToCommaDelimitedString(columnNames)
                + ") VALUES ("
                + StringUtils.collectionToCommaDelimitedString(columnParams)
                + ")");
    }

    public static Optional<String> createDeleteForTable(DataSource dataSource, String table) {
        StringBuilder keys = new StringBuilder();
        DatabaseInfo.listPrimaryKeys(dataSource, table).forEach(primaryKey -> {
            if (!keys.isEmpty()) {
                keys.append(" AND ");
            }
            keys.append(primaryKey.getColumnName() + "=:" + primaryKey.columnName);
        });
        return Optional.of("DELETE FROM " + table + " WHERE " + keys);
    }

    public static Optional<String> showCreateTable(DataSource dataSource, String table) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        if (!isCockroachDB(dataSource)) {
            return Optional.empty();
        }
        String createTable = null;
        try {
            createTable = template
                    .queryForObject("SELECT create_statement FROM [SHOW CREATE TABLE " + table + "]",
                            String.class).replaceAll("[\r\n\t]+", "");
        } catch (DataAccessException e) {
            // Assuming its related to escapes since JDBC metadata API removes surrounding quotes
            createTable = template
                    .queryForObject("SELECT create_statement FROM [SHOW CREATE TABLE \"" + table + "\"]",
                            String.class).replaceAll("[\r\n\t]+", "");
        }
        createTable = createTable.replace("CREATE TABLE", "CREATE TABLE IF NOT EXISTS");
        return Optional.of(createTable);
    }

    public static List<String> listTables(DataSource dataSource, String schema) {
        final List<String> tableNames = new ArrayList<>();
        try (Connection connection = DataSourceUtils.doGetConnection(dataSource)) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            ResultSet columns = databaseMetaData.getTables(null, null, null, new String[] {"TABLE"});
            while (columns.next()) {
                String tableSchema = columns.getString("TABLE_SCHEM");
                String tableType = columns.getString("TABLE_TYPE");
                if ("TABLE".equals(tableType) && schema.equals(tableSchema)) {
                    tableNames.add(columns.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Error reading table metadata", ex);
        }
        return tableNames;
    }

    public static List<ForeignKey> listForeignKeys(DataSource dataSource, String tableName) {
        final List<ForeignKey> foreignKeys = new ArrayList<>();
        try (Connection connection = DataSourceUtils.doGetConnection(dataSource)) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getImportedKeys(null, null, tableName);
            while (resultSet.next()) {
                foreignKeys.add(new ForeignKey(resultSet));
            }
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Error reading foreign key metadata", ex);
        }
        return foreignKeys;
    }

    public static List<PrimaryKey> listPrimaryKeys(DataSource dataSource, String tableName) {
        final List<PrimaryKey> primaryKeys = new ArrayList<>();
        try (Connection connection = DataSourceUtils.doGetConnection(dataSource)) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getPrimaryKeys(null, null, tableName);
            while (resultSet.next()) {
                primaryKeys.add(new PrimaryKey(resultSet));
            }
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Error reading primary key metadata", ex);
        }
        return primaryKeys;
    }

    public static Map<String, Column> listColumns(DataSource dataSource, String tableName) {
        final Map<String, Column> columns = new LinkedHashMap<>();
        try (Connection connection = DataSourceUtils.doGetConnection(dataSource)) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            // Surrounding quotes not supported / needed
            tableName = tableName.replaceAll("\"", "");
            ResultSet resultSet = databaseMetaData.getColumns(null, null, tableName, null);
            while (resultSet.next()) {
                String name = resultSet.getString("COLUMN_NAME");
                columns.put(name, new Column(name, resultSet));
            }
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Error reading table metadata", ex);
        }
        return columns;
    }
}
