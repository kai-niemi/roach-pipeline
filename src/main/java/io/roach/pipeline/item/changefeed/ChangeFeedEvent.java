package io.roach.pipeline.item.changefeed;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.roach.pipeline.util.LogicalTimestamp;

public class ChangeFeedEvent {
    @JsonProperty("payload")
    private List<Payload> payload;

    @JsonProperty("resolved")
    private String resolved;

    @JsonProperty("length")
    private int length;

    public List<Payload> getPayload() {
        return payload;
    }

    public String getResolved() {
        return resolved;
    }

    public int getLength() {
        return length;
    }

    public Optional<LogicalTimestamp> getResolvedTimestamp() {
        return resolved != null ? Optional.of(LogicalTimestamp.parse(resolved)) : Optional.empty();
    }
}
