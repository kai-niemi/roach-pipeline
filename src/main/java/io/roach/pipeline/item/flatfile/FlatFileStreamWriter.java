package io.roach.pipeline.item.flatfile;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class FlatFileStreamWriter<T> extends AbstractStreamWriter<T> {
    protected LineAggregator<T> lineAggregator;

    public FlatFileStreamWriter() {
        this.setExecutionContextName(ClassUtils.getShortName(FlatFileStreamWriter.class));
    }

    public void setLineAggregator(LineAggregator<T> lineAggregator) {
        this.lineAggregator = lineAggregator;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(lineAggregator, "A LineAggregator must be provided.");
    }

    @Override
    protected String doWrite(Chunk<? extends T> chunk) {
        StringBuilder lines = new StringBuilder();
        for (T item : chunk) {
            lines.append(this.lineAggregator.aggregate(item)).append(this.lineSeparator);
        }
        return lines.toString();
    }
}
