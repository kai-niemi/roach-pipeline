package io.roach.pipeline.item.flatfile.schema;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.transform.Range;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;

public class FlatFileSchemaTest {
    private static Gson createGson() {
        return new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
                .setPrettyPrinting()
                .create();
    }

    @Test
    public void whenWritingSchemaToJson_thenReadBackSuccessfully() throws Exception {
        FlatFileSchema flatFileSchema = new FlatFileSchema();
        {
            flatFileSchema.setName("test");
            flatFileSchema.getComments().addAll(Arrays.asList("--", "REM", "#"));
            flatFileSchema.addField(Field.fromName("ISIN").setRange(new Range(1, 12)));
            flatFileSchema.addField(Field.fromName("Quantity").setRange(new Range(13, 15)));
            flatFileSchema.addField(Field.fromName("Price").setRange(new Range(16, 20)));
            flatFileSchema.addField(Field.fromName("Customer").setRange(new Range(21, 29)));
        }

        {
            Tokenizer tokenizer = new Tokenizer();
            tokenizer.setType(Tokenizer.Type.fixed);
            tokenizer.setStrict(true);
            flatFileSchema.setTokenizer(tokenizer);
        }

        Path path = Paths.get("target/products-schema.json");

        try (FileWriter writer = new FileWriter(path.toFile())) {
            createGson().toJson(flatFileSchema, writer);
        } catch (JsonIOException e) {
            throw new IOException("Error writing", e);
        }

        Files.readAllLines(path).forEach(System.out::println);

        try (FileReader reader = new FileReader(path.toFile())) {
            FlatFileSchema schema = createGson().fromJson(reader, FlatFileSchema.class);
            System.out.println(schema);
        } catch (IllegalArgumentException | JsonParseException e) {
            throw new IOException("Error parsing schema", e);
        }
    }
}
