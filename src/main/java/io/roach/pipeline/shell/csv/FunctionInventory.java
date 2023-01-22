package io.roach.pipeline.shell.csv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.util.StringUtils;

import io.roach.pipeline.expression.MapRegistry;
import io.roach.pipeline.util.RandomData;

public final class FunctionInventory {
    private FunctionInventory() {
    }

    public static MapRegistry buildFunctions(DataSource dataSource) {
        MapRegistry registry = new MapRegistry();
        addRandomFunctions(registry);
        addIdFunctions(registry);
        addStringFunctions(registry);
        addDateFunctions(registry);
        addSQLFunctions(registry, dataSource);
        return registry;
    }

    public static void addRandomFunctions(MapRegistry registry) {
        // Random functions
        registry.addFunction("randomFirstName", args -> RandomData.randomFirstName());
        registry.addFunction("randomLastName", args -> RandomData.randomLastName());
        registry.addFunction("randomCity", args -> RandomData.randomCity());
        registry.addFunction("randomCountry", args -> RandomData.randomCountry());
        registry.addFunction("randomEmail", args -> RandomData.randomEmail());
        registry.addFunction("randomPhoneNumber", args -> RandomData.randomPhoneNumber());
        registry.addFunction("randomState", args -> RandomData.randomState());
        registry.addFunction("randomStateCode", args -> RandomData.randomStateCode());
        registry.addFunction("randomZipCode", args -> RandomData.randomZipCode());
        registry.addFunction("randomCurrency", args -> RandomData.randomCurrency());
        registry.addFunction("randomDate", args -> RandomData.randomDate());
        registry.addFunction("randomTime", args -> RandomData.randomTime());
        registry.addFunction("randomDateTime", args -> RandomData.randomDateTime());
        registry.addFunction("randomBigDecimal", args -> RandomData.randomBigDecimal());
        registry.addFunction("randomInt", args -> {
            Number arg1 = (Number) args[0];
            Number arg2 = (Number) args[0];
            return RandomData.randomInt(arg1.intValue(), arg2.intValue());
        });
        registry.addFunction("randomDouble", args -> {
            Number arg1 = (Number) args[0];
            Number arg2 = (Number) args[0];
            return RandomData.randomDouble(arg1.intValue(), arg2.intValue());
        });

        registry.addFunction("randomWord", args -> {
            Number arg1 = (Number) args[0];
            return RandomData.randomWord(arg1.intValue());
        });
        registry.addFunction("randomLoreIpsum", args -> {
            Number min = (Number) args[0];
            Number max = (Number) args[1];
            Boolean paragraphs = (Boolean) args[2];
            return RandomData.randomLoreIpsum(min.intValue(), max.intValue(), paragraphs);
        });
        registry.addFunction("randomJson", args -> {
            Number items = (Number) args[0];
            Number nested = (Number) args[1];
            return RandomData.randomJson(items.intValue(), nested.intValue());
        });
        registry.addFunction("randomMoney", args -> {
            String arg1 = (String) args[0];
            String arg2 = (String) args[0];
            String arg3 = (String) args[0];
            return RandomData.randomMoneyBetween(arg1, arg2, Currency.getInstance(arg3));
        });
    }

    public static void addIdFunctions(MapRegistry registry) {
        // ID generation functions
        registry.addFunction("uuid", args -> UUID.randomUUID());
    }

    public static void addDateFunctions(MapRegistry registry) {
        // Time and date functions
        registry.addFunction("time", args -> LocalTime.now());
        registry.addFunction("timePlus", args -> {
            Number amount = (Number) args[0];
            String unit = (String) args[1];
            return LocalTime.now().plus(amount.longValue(), ChronoUnit.valueOf(unit));
        });
        registry.addFunction("date", args -> LocalDate.now());
        registry.addFunction("datePlus", args -> {
            Number amount = (Number) args[0];
            String unit = (String) args[1];
            return LocalDate.now().plus(amount.longValue(), ChronoUnit.valueOf(unit));
        });
        registry.addFunction("timestamp", args -> LocalDateTime.now());
        registry.addFunction("timestampPlus", args -> {
            Number amount = (Number) args[0];
            String unit = (String) args[1];
            return LocalDateTime.now().plus(amount.longValue(), ChronoUnit.valueOf(unit));
        });
    }

    public static void addStringFunctions(MapRegistry registry) {
        // String
        registry.addFunction("lowerCase", args -> {
            String a = (String) args[0];
            return a.toLowerCase();
        });
        registry.addFunction("upperCase", args -> {
            String a = (String) args[0];
            return a.toUpperCase();
        });
        registry.addFunction("capitalize", args -> {
            String a = (String) args[0];
            return StringUtils.capitalize(a);
        });
    }

    public static void addSQLFunctions(MapRegistry registry, DataSource dataSource) {
        // SQL functions
        registry.addFunction("selectOne", args -> {
            LinkedList<Object> argsList = new LinkedList<>(Arrays.asList(args));
            String query = (String) argsList.pop();

            List<Object> result = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                int col = 1;
                for (Object a : argsList) {
                    ps.setObject(col++, a);
                }

                try (ResultSet res = ps.executeQuery()) {
                    while (res.next()) {
                        result.add(res.getString(1));
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }

            if (result.size() != 1) {
                throw new IllegalStateException("Expected 1 row got " + result.size());
            }

            return result.iterator().next();
        });
    }
}
