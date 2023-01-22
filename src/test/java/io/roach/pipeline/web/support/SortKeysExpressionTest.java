package io.roach.pipeline.web.support;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.database.Order;

import io.roach.pipeline.util.SortKeysExpression;

public class SortKeysExpressionTest {
    @Test
    public void whenParsingExpression_thenReturnStuff() {
        String keys = "id ASC, name DESC,age,y  ,z";
        Map<String, Order> sortConfiguration = SortKeysExpression.parse(keys);

        Assertions.assertEquals(5, sortConfiguration.size());
        Assertions.assertEquals(Order.ASCENDING, sortConfiguration.get("id"));
        Assertions.assertEquals(Order.DESCENDING, sortConfiguration.get("name"));
        Assertions.assertEquals(Order.ASCENDING, sortConfiguration.get("age"));
        Assertions.assertEquals(Order.ASCENDING, sortConfiguration.get("y"));
        Assertions.assertEquals(Order.ASCENDING, sortConfiguration.get("z"));
        Assertions.assertNull(sortConfiguration.get("zx"));

    }
}
