package io.roach.pipeline.item.flatfile.schema;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.batch.item.file.transform.RegexLineTokenizer;
import org.springframework.util.StringUtils;

public class FlatFileSchema {
    private String name;

    private String encoding = Charset.defaultCharset().name();

    private boolean strict;

    private List<String> comments = new ArrayList<>();

    private List<Field> fields = new ArrayList<>();

    private Tokenizer tokenizer = new Tokenizer()
            .setType(Tokenizer.Type.delimited);

    public String getName() {
        return name;
    }

    public FlatFileSchema setName(String name) {
        this.name = name;
        return this;
    }

    public String getEncoding() {
        return encoding;
    }

    public FlatFileSchema setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public boolean isStrict() {
        return strict;
    }

    public FlatFileSchema setStrict(boolean strict) {
        this.strict = strict;
        return this;
    }

    public List<String> getComments() {
        return comments;
    }

    public FlatFileSchema setComments(List<String> comments) {
        this.comments = comments;
        return this;
    }

    public List<Field> getIncludedFields() {
        return this.fields.stream()
                .filter(field -> !field.isIgnored()).collect(Collectors.toList());
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public List<Field> getFields() {
        return Collections.unmodifiableList(this.fields);
    }

    public FlatFileSchema setFields(List<Field> fields) {
        this.fields = fields;
        return this;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    public FlatFileSchema setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        return this;
    }

    public List<String> fieldNames() {
        List<String> fieldNames = new ArrayList<>();
        this.fields.forEach(field -> {
            if (!StringUtils.hasLength(field.getExpression()) && !field.isIgnored()) {
                fieldNames.add(field.getName());
            }
        });
        return fieldNames;
    }

    public int[] includedFields() {
        List<Integer> included = new ArrayList<>();
        AtomicInteger idx = new AtomicInteger();
        this.fields.forEach(field -> {
            if (!field.isIgnored()) {
                included.add(idx.get());
            }
            idx.incrementAndGet();
        });
        return included.stream().mapToInt(i -> i).toArray();
    }

    public List<String> allFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        this.fields.forEach(field -> fieldNames.add(field.getName()));
        return fieldNames;

    }

    public List<Range> fieldRanges() {
        List<Range> fieldRanges = new ArrayList<>();
        this.fields.forEach(field -> {
            if (!StringUtils.hasLength(field.getExpression())) {
                fieldRanges.add(field.getRange());
            }
        });
        return fieldRanges;
    }

    public LineTokenizer lineTokenizer() {
        final LineTokenizer lineTokenizer;

        if (tokenizer.getType() == Tokenizer.Type.regex) {
            RegexLineTokenizer regexLineTokenizer = new RegexLineTokenizer();
            regexLineTokenizer.setRegex(tokenizer.getPattern());
            regexLineTokenizer.setNames(fieldNames().toArray(new String[] {}));
            regexLineTokenizer.setStrict(isStrict());

            lineTokenizer = regexLineTokenizer;
        } else if (tokenizer.getType() == Tokenizer.Type.delimited) {
            DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
            delimitedLineTokenizer.setNames(fieldNames().toArray(new String[] {}));
            delimitedLineTokenizer.setIncludedFields(includedFields());
            delimitedLineTokenizer.setDelimiter(tokenizer.getDelimiter());
            delimitedLineTokenizer.setQuoteCharacter(tokenizer.getQuoteCharacter());
            delimitedLineTokenizer.setStrict(isStrict());

            lineTokenizer = delimitedLineTokenizer;
        } else if (tokenizer.getType() == Tokenizer.Type.fixed) {
            FixedLengthTokenizer fixedLengthTokenizer = new FixedLengthTokenizer();
            fixedLengthTokenizer.setNames(fieldNames().toArray(new String[] {}));
            fixedLengthTokenizer.setColumns(fieldRanges().toArray(new Range[] {}));
            fixedLengthTokenizer.setStrict(isStrict());

            lineTokenizer = fixedLengthTokenizer;
        } else {
            throw new IllegalStateException("Unknown tokenizer type: " + tokenizer.getType());
        }

        return lineTokenizer;
    }

    @Override
    public String toString() {
        return "FlatFileSchema{" +
                "name='" + name + '\'' +
                ", encoding='" + encoding + '\'' +
                ", strict=" + strict +
                ", comments=" + comments +
                ", fields=" + fields +
                ", tokenizer=" + tokenizer +
                '}';
    }
}
