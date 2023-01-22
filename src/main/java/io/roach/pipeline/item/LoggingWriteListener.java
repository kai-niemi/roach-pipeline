package io.roach.pipeline.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;

public class LoggingWriteListener<T> implements ItemWriteListener<T> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int chunks;

    private int itemsWritten;

    public LoggingWriteListener() {
    }

    @Override
    public void beforeWrite(Chunk<? extends T> items) {
    }

    @Override
    public void afterWrite(Chunk<? extends T> items) {
        itemsWritten += items.size();
        chunks++;
    }

    @Override
    public void onWriteError(Exception exception, Chunk<? extends T> items) {
        logger.error("Write error near chunk: {} written: {} items: {} error: {}",
                chunks, itemsWritten, items, exception.getLocalizedMessage());
    }
}
