package io.roach.pipeline.item.changefeed;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.util.Assert;

import io.roach.pipeline.config.Todo;

/**
 * An {@link org.springframework.batch.item.ItemReader} implementation for CockroachDB webhook CDC sink.
 * Uses a {@link PayloadConsumer} to read data from a given stream of change events, scoped by table.
 * One table change feed is mapped to one reader instance.
 */
public class ChangeFeedItemReader extends AbstractItemStreamItemReader<Payload> {
    private static final String LAST_KEYS = "last.keys";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Subscriber subscriber;

    private final ChangeFeedDispatcher changeFeedDispatcher;

    private boolean saveState = true;

    private boolean registrationFailed;

    private List<Object> lastKeys = new ArrayList<>();

    public ChangeFeedItemReader(Subscriber subscriber, ChangeFeedDispatcher changeFeedDispatcher) {
        Assert.notNull(subscriber, "subscriber must not be null");
        Assert.notNull(changeFeedDispatcher, "changeFeedDispatcher must not be null");

        this.subscriber = subscriber;
        this.changeFeedDispatcher = changeFeedDispatcher;
    }

    public boolean isSaveState() {
        return saveState;
    }

    public void setSaveState(boolean saveState) {
        this.saveState = saveState;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        Duration pollTimeout = subscriber.getPollTimeout();
        Assert.notNull(pollTimeout, "subscriber pollTimeout must not be null");

        logger.info("Change feed poll timeout: [{}]", pollTimeout);

        if (this.saveState && executionContext.containsKey(LAST_KEYS)) {
            this.lastKeys = new ArrayList<>((List<Object>) executionContext.get(LAST_KEYS));
            // Only used for logging
            if (logger.isDebugEnabled()) {
                logger.debug("Last seen keys: [" + lastKeys + "]");
            }
        }

        if (!this.changeFeedDispatcher.registerSubscriber(subscriber)) {
            this.registrationFailed = true;
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        if (this.saveState && !lastKeys.isEmpty()) {
            executionContext.put(LAST_KEYS, new ArrayList<>(this.lastKeys));
        }
    }

    @Override
    @Todo
    public Payload read() throws Exception {
        if (registrationFailed) {
            throw new JobExecutionException("Subcriber registration failed - possible duplicate ID");
        }
        Payload payload = changeFeedDispatcher.receive(subscriber);
        if (payload != null) {
            lastKeys = payload.getKeys();
            return payload;
        }
        logger.info("Change feed reader completion for subscriber: {}", subscriber);
        return null;
    }

    @Override
    public void close() {
// Dont unregister to allow job restarts
//        if (!registrationFailed) {
//            changeFeedDispatcher.unregisterSubscriber(subscriber);
//        }
    }
}
