package io.roach.pipeline.web.cdc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.roach.pipeline.item.changefeed.Subscriber;
import io.roach.pipeline.util.LogicalTimestamp;

@JsonPropertyOrder({"links", "embedded"})
public class SubscriberModel extends RepresentationModel<SubscriberModel> {
    public static SubscriberModel from(Subscriber subscriber) {
        return new SubscriberModel(subscriber);
    }

    private final String id;

    private final Duration pollTimeout;

    private final LocalDateTime creationTimestamp;

    private final LocalDateTime lastEventReceived;

    private final LogicalTimestamp lastResolvedTimestamp;

    private final String tableName;

    private final int eventsReceived;

    private final int payloadsReceived;

    public SubscriberModel(Subscriber subscriber) {
        this.id = subscriber.getId();
        this.pollTimeout = subscriber.getPollTimeout();
        this.creationTimestamp = subscriber.getCreationTimestamp();
        this.tableName = subscriber.getTableName();
        this.eventsReceived = subscriber.getEventsReceived();
        this.payloadsReceived = subscriber.getPayloadsReceived();
        this.lastEventReceived = subscriber.getLastEventReceivedTimestamp();
        this.lastResolvedTimestamp = subscriber.getLastResolvedTimestamp();
    }

    public String getId() {
        return id;
    }

    public Duration getPollTimeout() {
        return pollTimeout;
    }

    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public String getTableName() {
        return tableName;
    }

    public int getEventsReceived() {
        return eventsReceived;
    }

    public LocalDateTime getLastEventReceived() {
        return lastEventReceived;
    }

    public LogicalTimestamp getLastResolvedTimestamp() {
        return lastResolvedTimestamp;
    }

    public int getPayloadsReceived() {
        return payloadsReceived;
    }

    public Duration getDurationUntilPollExpires() {
        if (lastEventReceived == null) {
            return Duration.ofSeconds(0);
        }
        return Duration.ofSeconds(
                LocalDateTime.now().until(lastEventReceived.plus(pollTimeout), ChronoUnit.SECONDS));
    }
}
