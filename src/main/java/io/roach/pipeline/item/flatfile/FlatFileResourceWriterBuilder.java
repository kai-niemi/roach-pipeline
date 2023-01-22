package io.roach.pipeline.item.flatfile;

import java.util.List;
import java.util.Map;

import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.core.io.WritableResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public abstract class FlatFileResourceWriterBuilder {
    public static Builder instance() {
        return new Builder();
    }

    public static class Builder {
        private List<String> fieldNames;

        private WritableResource outputResource;

        private String delimiter = ",";

        public Builder setFieldNames(List<String> fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        public Builder setOutputResource(WritableResource outputResource) {
            this.outputResource = outputResource;
            return this;
        }

        public Builder setDelimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public FlatFileItemWriter<Map<String, Object>> build() {
            DelimitedLineAggregator<Map<String, Object>> lineAggregator = new DelimitedLineAggregator<>() {
                @Override
                public String doAggregate(Object[] fields) {
                    return super.doAggregate(fields);
                }
            };
            lineAggregator.setDelimiter(delimiter);

            return new FlatFileItemWriterBuilder<Map<String, Object>>()
                    .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                    .resource(outputResource)
                    .append(false)
                    .shouldDeleteIfExists(true)
                    .saveState(false)
                    .transactional(false)
                    .forceSync(false)
                    .lineAggregator(lineAggregator)
                    .headerCallback(writer -> writer
                            .write(StringUtils.collectionToDelimitedString(fieldNames, delimiter)))
                    .build();
        }
    }
}
