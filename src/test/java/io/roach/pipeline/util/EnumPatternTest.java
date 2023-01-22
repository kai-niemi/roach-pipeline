package io.roach.pipeline.util;

import java.util.EnumSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EnumPatternTest {
    @Test
    public void whenParsingEnumList_thenReturnEnums() {
        EnumSet<Color> options = EnumPattern.parse("red,green,blue", Color.class);
        Assertions.assertEquals(EnumSet.of(Color.red, Color.green, Color.blue), options);
    }

    @Test
    public void whenParsingEnumListWithPredicate_thenReturnEnums() {
        EnumSet<Color> options = EnumPattern.parse("red,green,bad", Color.class, s -> {
            try {
                Color.valueOf(s);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
        Assertions.assertEquals(EnumSet.of(Color.red, Color.green), options);
    }

    @Test
    public void whenParsingBadEnumList_thenThrowException() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            EnumPattern.parse("red,green,yellow", Color.class);
        }, "IllegalArgumentException was expected");

        Assertions.assertEquals("No enum constant io.roach.pipeline.util.Color.yellow", thrown.getMessage());
    }
}
