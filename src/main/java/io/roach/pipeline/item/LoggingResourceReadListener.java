package io.roach.pipeline.item;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.core.io.Resource;

public class LoggingResourceReadListener<T> implements ItemReadListener<T> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Resource inputResource;

    private final long totalLines;

    private long itemsRead;

    public LoggingResourceReadListener(Resource inputResource, boolean skipLineCount) {
        this.inputResource = inputResource;
        this.totalLines = skipLineCount ? 0 : countInputResourceLines();
    }

    private long countInputResourceLines() {
        // For streams
        if (!inputResource.isFile()) {
            logger.warn("Input resource not a file: {}", inputResource.getFilename());
            return 0;
        }

        logger.debug("Counting lines in: {}", inputResource.getFilename());

        try {
            long lineCount;
            try (Stream<String> stream = Files.lines(inputResource.getFile().toPath(), StandardCharsets.UTF_8)) {
                lineCount = stream.count();
            }
            logger.debug("Counted lines in {}: {}", inputResource.getFilename(), lineCount);
            return lineCount;
        } catch (IOException e) {
            logger.warn("I/O exception counting lines", e);
            return 0;
        }
    }

    @Override
    public void beforeRead() {
        this.itemsRead++;
    }

    @Override
    public void afterRead(T item) {
        if (itemsRead % 500 == 0 && totalLines > 0) {
            double p = itemsRead * 1f / totalLines * 100.0;
            int ticks = Math.max(0, (int) (30 * p / 100.0) - 1);
            System.out.printf(Locale.US, "\r%4.1f%%[%-30s] %s (%6s/%6s)",
                    p,
                    new String(new char[ticks]).replace('\0', '#') + ">",
                    inputResource.getFilename(),
                    itemsRead,
                    totalLines);
        }
    }

    @Override
    public void onReadError(Exception ex) {
        logger.error("Read error near item #{}: {}", itemsRead, ex);
    }
}
