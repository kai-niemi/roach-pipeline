package io.roach.pipeline.item.changefeed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = Payload.class)
public class Payload {
    public enum Operation {
        insert,
        update,
        delete
    }

    @JsonProperty("key")
    private List<Object> keys = new ArrayList<>();

    @JsonProperty("before")
    private Map<String, Object> before = new HashMap<>();

    @JsonProperty("after")
    private Map<String, Object> after = new HashMap<>();

    private String topic;

    private String updated;

    public List<Object> getKeys() {
        return keys;
    }

    public String getTopic() {
        return topic;
    }

    public String getUpdated() {
        return updated;
    }

    public Map<String, Object> getAfter() {
        return after;
    }

    public Map<String, Object> getBefore() {
        return before;
    }

    public Operation getOperation() {
        if (before == null) {
            after = Collections.emptyMap();
        }
        if (after == null) {
            after = Collections.emptyMap();
        }
        return before.isEmpty() && !after.isEmpty()
                ? Operation.insert : !before.isEmpty() && !after.isEmpty()
                ? Operation.update : Operation.delete;
    }
}
