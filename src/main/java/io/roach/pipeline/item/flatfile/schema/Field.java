package io.roach.pipeline.item.flatfile.schema;

import java.util.Objects;

import org.springframework.batch.item.file.transform.Range;

public class Field {
    public static Builder builder() {
        return new Builder();
    }

    public static Field fromName(String name) {
        return new Builder().withName(name).build();
    }

    public static final class Builder {
        private Field instance = new Field();

        private Builder() {
        }

        public Builder withName(String name) {
            instance.name = name;
            return this;
        }

        public Builder withRange(Range range) {
            instance.range = range;
            return this;
        }

        public Builder withExpression(String expression) {
            instance.expression = expression;
            return this;
        }

        public Builder withComment(String comment) {
            instance.comment = comment;
            return this;
        }

        public Builder withIgnored(boolean ignored) {
            instance.ignored = ignored;
            return this;
        }

        public Field build() {
            return instance;
        }
    }


    private String name;

    private Range range;

    private String expression;

    private String comment;

    private boolean ignored;

    public Field() {
    }

    public boolean isIgnored() {
        return ignored;
    }

    public Field setIgnored(boolean ignored) {
        this.ignored = ignored;
        return this;
    }

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public Range getRange() {
        return range;
    }

    public Field setRange(Range range) {
        this.range = range;
        return this;
    }

    public String getExpression() {
        return expression;
    }

    public Field setExpression(String expression) {
        this.expression = expression;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public Field setComment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Field field = (Field) o;
        return name.equals(field.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Field{" +
                "name='" + name + '\'' +
                ", range=" + range +
                ", ignored=" + ignored +
                ", expression='" + expression + '\'' +
                '}';
    }
}
