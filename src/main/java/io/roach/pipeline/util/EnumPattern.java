package io.roach.pipeline.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public abstract class EnumPattern {
    private EnumPattern() {
    }

    public static <E extends Enum<E>> EnumSet<E> parse(String text, Class<E> enumType) {
        return parse(text, enumType, s -> true);
    }

    public static <E extends Enum<E>> EnumSet<E> parse(String text,
                                                       Class<E> enumType,
                                                       Predicate<String> predicate) {
        List<E> matches = new ArrayList<>();

        String key = text.trim();
        Arrays.stream(key.split(",")).forEach(s -> {
            if (predicate.test(s)) {
                matches.add(Enum.valueOf(enumType, s));
            }
        });

        if (matches.isEmpty()) {
            return EnumSet.noneOf(enumType);
        }

        return EnumSet.copyOf(matches);
    }
}
