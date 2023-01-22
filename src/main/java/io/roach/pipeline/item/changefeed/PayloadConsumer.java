package io.roach.pipeline.item.changefeed;

public interface PayloadConsumer {
    Payload receive(Subscriber subscriber);
}
