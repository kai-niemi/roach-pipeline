package io.roach.pipeline.item.changefeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class CompositeItemWriter implements ItemWriter<Payload> {
    private final ItemWriter<Map<String, Object>> insertWriter;

    private final ItemWriter<Map<String, Object>> deleteWriter;

    public CompositeItemWriter(ItemWriter<Map<String, Object>> insertWriter,
                               ItemWriter<Map<String, Object>> deleteWriter) {
        this.insertWriter = insertWriter;
        this.deleteWriter = deleteWriter;
    }

    @Override
    public void write(Chunk<? extends Payload> chunk) throws Exception {
        List<Map<String, Object>> inserts = new ArrayList<>();
        List<Map<String, Object>> deletes = new ArrayList<>();

        for (Payload payload : chunk) {
            if (payload.getOperation().equals(Payload.Operation.delete)) {
                payload.getKeys().forEach(k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", k);
                    deletes.add(m);
                });
            } else {
                inserts.add(payload.getAfter());
            }
        }

        if (!inserts.isEmpty()) {
            insertWriter.write(new Chunk(inserts));
        }
        if (!deletes.isEmpty()) {
            deleteWriter.write(new Chunk(deletes));
        }
    }
}
