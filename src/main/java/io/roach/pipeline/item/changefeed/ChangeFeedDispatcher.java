package io.roach.pipeline.item.changefeed;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.roach.pipeline.util.LogicalTimestamp;

@Component
public class ChangeFeedDispatcher implements PayloadConsumer, ChangeFeedProducer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<Subscriber, BlockingDeque<Payload>> queueMap = new HashMap<>();

    private final Set<String> latch = Collections.synchronizedSet(new HashSet<>());

    @Value("${pipeline.cdc.webhook.queue-capacity}")
    private int queueCapacity;

    public ChangeFeedDispatcher() {
    }

    @PostConstruct
    protected void init() {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity <= 0");
        }
    }

    public boolean registerSubscriber(Subscriber subscriber) {
        if (queueMap.putIfAbsent(subscriber, new LinkedBlockingDeque<>(queueCapacity)) == null) {
            logger.info("Registering subscriber [{}] with queueCapacity [{}]", subscriber, queueCapacity);
            latch.add(subscriber.getId());
            return true;
        } else {
            logger.warn("Subscriber [{}] already registered", subscriber);
        }
        return false;
    }

    public void unregisterSubscriber(Subscriber subscriber) {
        if (queueMap.remove(subscriber) != null) {
            logger.info("Unregistering subscriber [{}]", subscriber);
            latch.remove(subscriber.getId());
        } else {
            logger.warn("Subscriber not registered [{}]", subscriber);
        }
    }

    public Optional<Subscriber> getSubscriber(String id) {
        for (Subscriber s : queueMap.keySet()) {
            if (id.equals(s.getId())) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    public Payload receive(Subscriber subscriber) {
        BlockingDeque<Payload> blockingDeque = queueMap.get(subscriber);
        if (blockingDeque == null) {
            throw new SubscriberNotFound(subscriber);
        }
        try {
            String id = subscriber.getId();
            // Wait indefinitely for first event
            if (latch.remove(id)) {
                logger.info("Subscriber [{}] waiting for initial event", subscriber);
                Payload payload = blockingDeque.takeFirst();
                logger.info("Subscriber [{}] received initial event", subscriber);
                return payload;
            }
            Duration pollTimeout = subscriber.getPollTimeout();
            return blockingDeque.pollFirst(pollTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public int getQueueSize(Subscriber subscriber) {
        BlockingDeque<Payload> blockingDeque = queueMap.get(subscriber);
        if (blockingDeque != null) {
            return blockingDeque.size();
        }
        return 0;
    }

    public double getQueueCapacity(Subscriber subscriber) {
        int qSize = getQueueSize(subscriber);
        return qSize > 0 ? qSize / (queueCapacity * 1.0d) : 0;
    }

    @Override
    public void resolved(Subscriber subscriber, LogicalTimestamp resolvedTimestamp) {
        subscriber.setLastResolvedTimestamp(resolvedTimestamp);
        logger.debug("Last resolved timestamp for subscriber [{}]: {}",
                subscriber.getId(), resolvedTimestamp);
    }

    @Override
    public void publish(Subscriber subscriber, ChangeFeedEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Subscriber [{}] queue size [{}] / capacity [{}]",
                    subscriber.getId(),
                    getQueueSize(subscriber),
                    getQueueCapacity(subscriber) * 100.0 + "%"
            );
        }

        BlockingDeque<Payload> blockingDeque = queueMap.get(subscriber);
        if (blockingDeque == null) {
            throw new SubscriberNotFound(subscriber);
        }

        subscriber.incrementPayloadReceived(event.getPayload());

        for (Payload payload : event.getPayload()) {
            try {
                blockingDeque.putLast(payload);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
