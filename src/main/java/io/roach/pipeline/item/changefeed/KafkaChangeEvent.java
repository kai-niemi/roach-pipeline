package io.roach.pipeline.item.changefeed;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.roach.pipeline.util.LogicalTimestamp;

@JsonDeserialize(as = KafkaChangeEvent.class)
public class KafkaChangeEvent {
    private String resolved;

    private String updated;

    @JsonProperty("after")
    private Map<String, Object> after = new HashMap<>();

    public String getResolved() {
        return resolved;
    }

    public Optional<LogicalTimestamp> getResolvedTimestamp() {
        return resolved != null ? Optional.of(LogicalTimestamp.parse(resolved)) : Optional.empty();
    }

    public String getUpdated() {
        return updated;
    }

    public Optional<LogicalTimestamp> getUpdatedTimestamp() {
        return updated != null ? Optional.of(LogicalTimestamp.parse(updated)) : Optional.empty();
    }

    public Map<String, Object> getAfter() {
        return Collections.unmodifiableMap(after);
    }
}
