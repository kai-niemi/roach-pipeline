package io.roach.pipeline.item.flatfile;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.core.io.Resource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import io.roach.pipeline.item.flatfile.schema.Field;
import io.roach.pipeline.item.flatfile.schema.FlatFileSchema;

public abstract class FlatFileReaderBuilder {
    public static Builder instance() {
        return new Builder();
    }

    public static class Builder {
        private FlatFileSchema flatFileSchema;

        private int linesToSkip;

        private Resource inputResource;

        public Builder setFlatFileSchema(FlatFileSchema flatFileSchema) {
            this.flatFileSchema = flatFileSchema;
            return this;
        }

        public Builder setLinesToSkip(int linesToSkip) {
            this.linesToSkip = linesToSkip;
            return this;
        }

        public Builder setInputResource(Resource inputResource) {
            this.inputResource = inputResource;
            return this;
        }

        public ItemReader<Map<String, Object>> build() {
            if (inputResource == null) {
                throw new IllegalStateException("No target output ");
            }
            if (flatFileSchema == null) {
                throw new IllegalStateException("No source schema");
            }

            return new FlatFileItemReaderBuilder<Map<String, Object>>()
                    .comments(flatFileSchema.getComments().toArray(new String[] {}))
                    .linesToSkip(linesToSkip)
                    .encoding(flatFileSchema.getEncoding())
                    .resource(inputResource)
                    .strict(flatFileSchema.isStrict())
                    .saveState(false)
                    .lineMapper(createLineMapper())
                    .build();
        }

        private LineMapper<Map<String, Object>> createLineMapper() {
            final ExpressionParser expressionParser = new SpelExpressionParser();

            final LineTokenizer lineTokenizer = flatFileSchema.lineTokenizer();

            final List<Field> fields = Collections.unmodifiableList(flatFileSchema.getIncludedFields());

            final Map<Field, Expression> expressionMap = new HashMap<>();

            fields.forEach(field -> {
                if (StringUtils.hasLength(field.getExpression())) {
                    Expression expression = expressionParser.parseExpression(field.getExpression());
                    expressionMap.put(field, expression);
                }
            });

            DefaultLineMapper<Map<String, Object>> lineMapper = new DefaultLineMapper<Map<String, Object>>() {
                @Override
                public Map<String, Object> mapLine(String line, int lineNumber) throws Exception {
                    if (StringUtils.hasLength(line)) {
                        return super.mapLine(line, lineNumber);
                    } else {
                        return Collections.emptyMap();
                    }
                }
            };
            lineMapper.setLineTokenizer(lineTokenizer);
            lineMapper.setFieldSetMapper(fieldSet -> {
                final Map<String, Object> finalValues = new LinkedHashMap<>();

                if (!expressionMap.isEmpty()) {
                    final EvaluationContext context = new StandardEvaluationContext();
                    context.setVariable("fieldSet", fieldSet);
                    context.setVariable("names", fieldSet.getNames());
                    context.setVariable("values", fieldSet.getValues());

                    fields.forEach(field -> {
                        if (expressionMap.containsKey(field)) {
                            finalValues.put(field.getName(), expressionMap.get(field).getValue(context, String.class));
                        } else {
                            finalValues.put(field.getName(), fieldSet.readString(field.getName()));
                        }
                    });
                } else {
                    fields.forEach(field -> finalValues.put(field.getName(), fieldSet.readString(field.getName())));
                }

                return finalValues;
            });

            return lineMapper;
        }
    }
}
