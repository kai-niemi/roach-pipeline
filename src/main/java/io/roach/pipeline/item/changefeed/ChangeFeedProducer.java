package io.roach.pipeline.item.changefeed;

import io.roach.pipeline.util.LogicalTimestamp;

public interface ChangeFeedProducer {
    void resolved(Subscriber subscriber, LogicalTimestamp resolvedTimestamp);

    void publish(Subscriber subscriber, ChangeFeedEvent event);
}
