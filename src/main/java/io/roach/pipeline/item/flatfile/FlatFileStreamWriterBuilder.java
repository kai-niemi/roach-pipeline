package io.roach.pipeline.item.flatfile;

import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.util.StringUtils;

public class FlatFileStreamWriterBuilder {
    public static Builder instance() {
        return new Builder();
    }

    public static class Builder {
        private List<String> fieldNames;

        private Writer outputWriter;

        private String delimiter = ",";

        public Builder setFieldNames(List<String> fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        public Builder setOutputWriter(Writer outputWriter) {
            this.outputWriter = outputWriter;
            return this;
        }

        public Builder setDelimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public ItemWriter<Map<String, Object>> build() {
            DelimitedLineAggregator<Map<String, Object>> lineAggregator = new DelimitedLineAggregator<>();
            lineAggregator.setDelimiter(delimiter);

            FlatFileStreamWriter<Map<String, Object>> itemWriter = new FlatFileStreamWriter<>();
            itemWriter.setLineAggregator(lineAggregator);
            itemWriter.setOutputBufferedWriter(outputWriter);
            itemWriter.setHeaderCallback(writer -> writer
                    .write(StringUtils.collectionToDelimitedString(fieldNames, delimiter))
            );

            return itemWriter;
        }
    }
}
