package io.roach.pipeline.item.flatfile.schema;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;

import javax.sql.DataSource;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;

public abstract class FlatFileSchemaUtils {
    private FlatFileSchemaUtils() {
    }

    public static FlatFileSchema generateSchema(DataSource dataSource,
                                                String tableName,
                                                String delimiter,
                                                Predicate<Field> predicate) {
        FlatFileSchema flatFileSchema = new FlatFileSchema();
        flatFileSchema.setTokenizer(new Tokenizer()
                .setType(Tokenizer.Type.delimited)
                .setDelimiter(delimiter)
                .setStrict(true));
        flatFileSchema.setName(tableName);
        flatFileSchema.getComments().addAll(Arrays.asList("--", "REM", "#"));

        RowMapper<?> rowMapper = (rs, rowNum) -> {
            String columnName = rs.getString("column_name");
            String comment = rs.getString("comment");
            Field field = Field.builder()
                    .withName(columnName)
                    .withComment(comment)
                    .build();
            if (predicate.test(field)) {
                flatFileSchema.addField(field);
            }
            return null;
        };

        JdbcTemplate template = new JdbcTemplate(dataSource);
        try {
            template.query("select column_name,comment from [SHOW COLUMNS FROM " + tableName + " WITH COMMENT]", rowMapper);
        } catch (BadSqlGrammarException e) {
            template.query("select column_name,comment from [SHOW COLUMNS FROM \"" + tableName + "\" WITH COMMENT]", rowMapper);
        }

        return flatFileSchema;
    }

    private static Gson createGson() {
        return new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
                .setPrettyPrinting()
                .create();
    }

    public static void writeToFile(FlatFileSchema schema, Path path)
            throws IOException {
        try (FileWriter writer = new FileWriter(path.toFile())) {
            createGson().toJson(schema, writer);
        } catch (JsonIOException e) {
            throw new IOException("Error writing schema", e);
        }
    }

    public static void writeToStream(FlatFileSchema schema, OutputStream out)
            throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
            createGson().toJson(schema, writer);
        } catch (JsonIOException e) {
            throw new IOException("Error writing schema", e);
        }
    }

    public static FlatFileSchema readFromFile(Path path)
            throws IOException {
        try (FileReader reader = new FileReader(path.toFile())) {
            return createGson().fromJson(reader, FlatFileSchema.class);
        } catch (IllegalArgumentException | JsonParseException e) {
            throw new IOException("Error reading schema", e);
        }
    }

    public static FlatFileSchema readFromStream(InputStream input)
            throws IOException {
        try (InputStreamReader reader = new InputStreamReader(input)) {
            return createGson().fromJson(reader, FlatFileSchema.class);
        } catch (IllegalArgumentException | JsonParseException e) {
            throw new IOException("Error reading schema", e);
        }
    }
}
