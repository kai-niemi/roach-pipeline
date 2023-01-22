package io.roach.pipeline.item.changefeed;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.Assert;

import io.roach.pipeline.util.LogicalTimestamp;

public class Subscriber implements Comparable<Subscriber> {
    private final String id;

    private Duration pollTimeout;

    private String tableName;

    private LocalDateTime creationTimestamp;

    private LogicalTimestamp lastResolvedTimestamp;

    private LocalDateTime lastEventReceivedTimestamp;

    private final AtomicInteger eventsReceived = new AtomicInteger();

    private final AtomicInteger payloadsReceived = new AtomicInteger();

    private List<Payload> lastPayload = new ArrayList<>();

    public Subscriber(String id) {
        Assert.notNull(id, "id is null");
        this.id = id;
    }

    public Subscriber incrementPayloadReceived(List<Payload> payloads) {
        this.lastEventReceivedTimestamp = LocalDateTime.now();
        this.eventsReceived.incrementAndGet();
        this.payloadsReceived.addAndGet(payloads.size());
        this.lastPayload = new ArrayList<>(payloads);
        return this;
    }

    public String getId() {
        return id;
    }

    public List<Payload> getLastPayload() {
        return lastPayload;
    }

    public Duration getPollTimeout() {
        return pollTimeout;
    }

    public Subscriber setPollTimeout(Duration pollTimeout) {
        this.pollTimeout = pollTimeout;
        Assert.isTrue(!pollTimeout.isZero(), "pollTimeout must not be zero");
        Assert.isTrue(!pollTimeout.isNegative(), "pollTimeout must not be negative");
        return this;
    }

    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public Subscriber setCreationTimestamp(LocalDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public Subscriber setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public int getEventsReceived() {
        return eventsReceived.get();
    }

    public int getPayloadsReceived() {
        return payloadsReceived.get();
    }

    public LogicalTimestamp getLastResolvedTimestamp() {
        return lastResolvedTimestamp;
    }

    public Subscriber setLastResolvedTimestamp(LogicalTimestamp lastResolvedTimestamp) {
        this.lastResolvedTimestamp = lastResolvedTimestamp;
        return this;
    }

    public LocalDateTime getLastEventReceivedTimestamp() {
        return lastEventReceivedTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Subscriber that = (Subscriber) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(Subscriber o) {
        return id.compareTo(o.id);
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "id=" + id +
                ", pollTimeout=" + pollTimeout +
                ", tableName='" + tableName + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                ", lastResolvedTimestamp=" + lastResolvedTimestamp +
                ", lastEventReceivedTimestamp=" + lastEventReceivedTimestamp +
                ", eventsReceived=" + eventsReceived +
                ", payloadsReceived=" + payloadsReceived +
                '}';
    }
}
