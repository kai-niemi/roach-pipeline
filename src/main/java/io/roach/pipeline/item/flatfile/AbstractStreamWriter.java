package io.roach.pipeline.item.flatfile;

import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractStreamWriter<T> extends AbstractItemStreamItemWriter<T> implements InitializingBean {
    public static final String DEFAULT_LINE_SEPARATOR = System.getProperty("line.separator");

    protected static final Logger logger = LoggerFactory.getLogger(AbstractFileItemWriter.class);

    protected String lineSeparator = DEFAULT_LINE_SEPARATOR;

    private Writer outputBufferedWriter;

    private FlatFileHeaderCallback headerCallback;

    private FlatFileFooterCallback footerCallback;

    public AbstractStreamWriter<T> setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    public void setOutputBufferedWriter(Writer outputBufferedWriter) {
        this.outputBufferedWriter = outputBufferedWriter;
    }

    public void setHeaderCallback(FlatFileHeaderCallback headerCallback) {
        this.headerCallback = headerCallback;
    }

    public void setFooterCallback(FlatFileFooterCallback footerCallback) {
        this.footerCallback = footerCallback;
    }

    /**
     * Writes out a string followed by a "new line", where the format of the new
     * line separator is determined by the underlying operating system.
     *
     * @param chunk list of items to be written to output stream
     * @throws WriteFailedException if an error occurs while writing items to the output stream
     */
    @Override
    public void write(Chunk<? extends T> chunk) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Writing to file with " + chunk.size() + " items.");
        }

        String lines = doWrite(chunk);
        try {
            outputBufferedWriter.write(lines);
            outputBufferedWriter.flush();
        } catch (IOException e) {
            throw new WriteFailedException("Could not write data. The stream may be corrupt.", e);
        }
    }

    /**
     * Write out a string of items followed by a "new line", where the format of the new
     * line separator is determined by the underlying operating system.
     *
     * @param chunk to be written
     * @return written lines
     */
    protected abstract String doWrite(Chunk<? extends T> chunk);

    /**
     * @see ItemStream#close()
     */
    @Override
    public void close() {
        super.close();
        try {
            if (footerCallback != null) {
                footerCallback.writeFooter(outputBufferedWriter);
                outputBufferedWriter.flush();
            }
        } catch (IOException e) {
            throw new ItemStreamException("Failed to write footer before closing", e);
        }
    }

    /**
     * @see ItemStream#open(ExecutionContext)
     */
    @Override
    public void open(ExecutionContext executionContext) {
        super.open(executionContext);

        if (headerCallback != null) {
            try {
                headerCallback.writeHeader(outputBufferedWriter);
                outputBufferedWriter.write(lineSeparator);
            } catch (IOException e) {
                throw new ItemStreamException("Could not write headers.  The file may be corrupt.", e);
            }
        }
    }
}
