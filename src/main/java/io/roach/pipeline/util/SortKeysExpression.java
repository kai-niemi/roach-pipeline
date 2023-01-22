package io.roach.pipeline.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.batch.item.database.Order;

public abstract class SortKeysExpression {
    private SortKeysExpression() {
    }

    public static Map<String, Order> parse(String keys) {
        Pattern p = Pattern.compile("^(\\w+)\\s(ASC|DESC)?");

        Map<String, Order> sortConfiguration = new LinkedHashMap<>(); // Order is significant

        for (String tuple : keys.split(",")) {
            Matcher m = p.matcher(tuple.trim());
            if (m.matches()) {
                String key = m.group(1).trim();
                String order = m.group(2).toUpperCase();
                if ("ASC".equals(order)) {
                    order = Order.ASCENDING.name();
                } else if ("DESC".equals(order)) {
                    order = Order.DESCENDING.name();
                }
                sortConfiguration.put(key, Order.valueOf(order));
            } else {
                sortConfiguration.put(tuple.trim(), Order.ASCENDING);
            }
        }

        return sortConfiguration;
    }
}
