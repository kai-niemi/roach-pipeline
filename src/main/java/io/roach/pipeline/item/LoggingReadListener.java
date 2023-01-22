package io.roach.pipeline.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemReadListener;

public class LoggingReadListener<T> implements ItemReadListener<T> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private int itemsRead;

    public LoggingReadListener() {
    }

    @Override
    public void beforeRead() {
        this.itemsRead++;
    }

    @Override
    public void afterRead(T item) {
    }

    @Override
    public void onReadError(Exception ex) {
        logger.error("Read error near item: {} error: {}",
                itemsRead, ex.getLocalizedMessage());
    }
}
