package io.roach.pipeline.util.random;

import org.junit.jupiter.api.Test;

import io.roach.pipeline.util.RandomData;

public class RandomDataTest {
    @Test
    public void randomJson() {
        System.out.println(RandomData.randomJson(2, 2));
    }
}
