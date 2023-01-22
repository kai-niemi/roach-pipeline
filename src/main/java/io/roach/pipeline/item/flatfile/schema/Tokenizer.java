package io.roach.pipeline.item.flatfile.schema;

public class Tokenizer {
    public enum Type {
        regex, fixed, delimited
    }

    private Type type;

    private String pattern;

    private boolean strict;

    private String delimiter = ";";

    private char quoteCharacter = '"';

    public Type getType() {
        return type;
    }

    public Tokenizer setType(Type type) {
        this.type = type;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public Tokenizer setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public boolean isStrict() {
        return strict;
    }

    public Tokenizer setStrict(boolean strict) {
        this.strict = strict;
        return this;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public Tokenizer setDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    public char getQuoteCharacter() {
        return quoteCharacter;
    }

    public Tokenizer setQuoteCharacter(char quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
        return this;
    }

    @Override
    public String toString() {
        return "Tokenizer{" +
                "type=" + type +
                ", pattern='" + pattern + '\'' +
                ", strict=" + strict +
                ", delimiter='" + delimiter + '\'' +
                ", quoteCharacter=" + quoteCharacter +
                '}';
    }
}
