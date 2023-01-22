package io.roach.pipeline.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.roach.pipeline.shell.support.DatabaseInfo;

public class RandomData {
    private static final Logger logger = LoggerFactory.getLogger(RandomData.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    private static final List<String> firstNames = new ArrayList<>();

    private static final List<String> lastNames = new ArrayList<>();

    private static final List<String> cities = new ArrayList<>();

    private static final List<String> countries = new ArrayList<>();

    private static final List<String> currencies = new ArrayList<>();

    private static final List<String> states = new ArrayList<>();

    private static final List<String> stateCodes = new ArrayList<>();

    private static final List<String> lorem = new ArrayList<>();

    static {
        firstNames.addAll(readLines("random/firstnames.txt"));
        lastNames.addAll(readLines(("random/surnames.txt")));
        cities.addAll(readLines(("random/cities.txt")));
        states.addAll(readLines(("random/states.txt")));
        stateCodes.addAll(readLines(("random/state_code.txt")));
        lorem.addAll(readLines(("random/lorem.txt")));

        for (Locale locale : Locale.getAvailableLocales()) {
            if (StringUtils.hasLength(locale.getDisplayCountry(Locale.US))) {
                countries.add(locale.getDisplayCountry(Locale.US));
            }
        }

        for (Currency currency : Currency.getAvailableCurrencies()) {
            currencies.add(currency.getCurrencyCode());
        }
    }

    private static List<String> readLines(String path) {
        try (InputStream resource = new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("", e);
        }
        return Collections.emptyList();
    }

    public static BigDecimal randomBigDecimal() {
        return new BigDecimal(random.nextDouble(0, Double.MAX_VALUE));
    }

    public static Money randomMoneyBetween(String low, String high, Currency currency) {
        return randomMoneyBetween(Double.parseDouble(low), Double.parseDouble(high), currency);
    }

    public static Money randomMoneyBetween(double low, double high, Currency currency) {
        if (high <= low) {
            throw new IllegalArgumentException("high<=low");
        }
        return Money.of(String.format(Locale.US, "%.2f", random.nextDouble(low, high)), currency);
    }

    public static <T extends Enum<?>> T selectRandom(Class<T> clazz) {
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    public static <E> E selectRandom(List<E> collection) {
        return collection.get(random.nextInt(collection.size()));
    }

    public static <K> K selectRandom(Set<K> set) {
        Object[] keys = set.toArray();
        return (K) keys[random.nextInt(keys.length)];
    }

    public static <E> E selectRandom(E[] collection) {
        return collection[random.nextInt(collection.length)];
    }

    public static <E> Collection<E> selectRandomUnique(List<E> collection, int count) {
        if (count > collection.size()) {
            throw new IllegalArgumentException("Not enough elements");
        }

        Set<E> uniqueElements = new HashSet<>();
        while (uniqueElements.size() < count) {
            uniqueElements.add(selectRandom(collection));
        }

        return uniqueElements;
    }

    public static <E> Collection<E> selectRandomUnique(E[] array, int count) {
        if (count > array.length) {
            throw new IllegalArgumentException("Not enough elements");
        }

        Set<E> uniqueElements = new HashSet<>();
        while (uniqueElements.size() < count) {
            uniqueElements.add(selectRandom(array));
        }

        return uniqueElements;
    }

    public static <E extends WeightedItem> E selectRandomWeighted(Collection<E> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Empty collection");
        }
        double totalWeight = items.stream().mapToDouble(WeightedItem::getWeight).sum();
        double randomWeight = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;

        for (E item : items) {
            cumulativeWeight += item.getWeight();
            if (cumulativeWeight >= randomWeight) {
                return item;
            }
        }

        throw new IllegalStateException("This is not possible");
    }

    public static <T> T selectRandomWeighted(Collection<T> items, List<Double> weights) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Empty collection");
        }
        if (items.size() != weights.size()) {
            throw new IllegalArgumentException("Collection and weights mismatch");
        }

        double totalWeight = weights.stream().mapToDouble(w -> w).sum();
        double randomWeight = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;

        int idx = 0;
        for (T item : items) {
            cumulativeWeight += weights.get(idx++);
            if (cumulativeWeight >= randomWeight) {
                return item;
            }
        }

        throw new IllegalStateException("This is not possible");
    }

    public static String randomJson(int items, int nestedItems) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode users = root.putArray("users");

        IntStream.range(0, items).forEach(value -> {
            ObjectNode u = users.addObject();
            u.put("email", randomEmail());
            u.put("firstName", randomFirstName());
            u.put("lastName", randomLastName());
            u.put("telephone", randomPhoneNumber());
            u.put("userName", randomFirstName().toLowerCase());

            ArrayNode addr = u.putArray("addresses");
            IntStream.range(0, nestedItems).forEach(n -> {
                ObjectNode a = addr.addObject();
                a.put("state", randomState());
                a.put("stateCode", randomStateCode());
                a.put("city", randomCity());
                a.put("country", randomCountry());
                a.put("zipCode", randomZipCode());
            });

            users.add(u);
        });

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int randomInt(int start, int end) {
        return random.nextInt(start, end);
    }

    public static double randomDouble(double start, double end) {
        return random.nextDouble(start, end);
    }

    public static String randomFirstName() {
        return selectRandom(firstNames);
    }

    public static String randomLastName() {
        return selectRandom(lastNames);
    }

    public static String randomCity() {
        return StringUtils.capitalize(selectRandom(cities));
    }

    public static LocalDate randomDate() {
        return LocalDate.now().plus(random.nextBoolean() ? randomInt(15, 45) : randomInt(-15, -45), ChronoUnit.DAYS);
    }

    public static LocalTime randomTime() {
        return LocalTime.now().plus(random.nextBoolean() ? randomInt(12, 24) : randomInt(-12, -24), ChronoUnit.HOURS);
    }

    public static LocalDateTime randomDateTime() {
        return LocalDateTime.now().plus(random.nextBoolean() ? 12 : -12, ChronoUnit.DAYS);
    }

    public static String randomPhoneNumber() {
        StringBuilder sb = new StringBuilder()
                .append("(")
                .append(random.nextInt(9) + 1);
        for (int i = 0; i < 2; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append(") ")
                .append(random.nextInt(9) + 1);
        for (int i = 0; i < 2; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomCountry() {
        return selectRandom(countries);
    }

    public static String randomCurrency() {
        return selectRandom(currencies);
    }

    public static String randomState() {
        return selectRandom(states);
    }

    public static String randomStateCode() {
        return selectRandom(stateCodes);
    }

    public static String randomZipCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomEmail() {
        String sb = randomFirstName().toLowerCase()
                + "."
                + randomLastName().toLowerCase()
                + "@example.com";
        return sb.replace(' ', '.');
    }

    public static String randomLoreIpsum(int min, int max, boolean paragraphs) {
        return new LoreIpsum(min, max, paragraphs).generate();
    }

    private static final char[] VOWELS = "aeiou".toCharArray();

    private static final char[] CONSONANTS = "bcdfghjklmnpqrstvwxyz".toCharArray();

    public static String randomWord(int min) {
        StringBuilder sb = new StringBuilder();
        boolean vowelStart = true;
        for (int i = 0; i < min; i++) {
            if (vowelStart) {
                sb.append(VOWELS[(int) (random.nextDouble() * VOWELS.length)]);
            } else {
                sb.append(CONSONANTS[(int) (random.nextDouble() * CONSONANTS.length)]);
            }
            vowelStart = !vowelStart;
        }
        return sb.toString();
    }

    private static class LoreIpsum {
        private int min = 2;

        private int max = 5;

        private final boolean paragraphs;

        public LoreIpsum(int min, int max, boolean paragraphs) {
            this.min = min;
            this.max = max;
            this.paragraphs = paragraphs;
        }

        public String generate() {
            return StringUtils.capitalize(paragraphs ? getParagraphs(min, max) : getWords(getCount(min, max), false));
        }

        public String getParagraphs(int min, int max) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < getCount(min, max); j++) {
                for (int i = 0; i < random.nextInt(5) + 2; i++) {
                    sb.append(StringUtils.capitalize(getWords(1, false)))
                            .append(getWords(getCount(2, 20), false))
                            .append(". ");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        }

        private int getCount(int min, int max) {
            min = Math.max(0, min);
            if (max < min) {
                max = min;
            }
            return max != min ? random.nextInt(max - min) + min : min;
        }

        private String getWords(int count, boolean capitalize) {
            StringBuilder sb = new StringBuilder();

            int wordCount = 0;
            while (wordCount < count) {
                String word = lorem.get(random.nextInt(lorem.size()));
                if (capitalize) {
                    if (wordCount == 0 || word.length() > 3) {
                        word = StringUtils.capitalize(word);
                    }
                }
                sb.append(word);
                sb.append(" ");
                wordCount++;
            }
            return sb.toString().trim();
        }
    }

    public static Object randomValue(DatabaseInfo.Column metaData) {
        int datatype = Integer.parseInt(metaData.getAttribute("DATA_TYPE"));
        int precision = Integer.parseInt(metaData.getAttribute("COLUMN_SIZE"));
        if (precision == Integer.MAX_VALUE) {
            precision = 256;
        }
        switch (datatype) {
            case java.sql.Types.BIT:
            case java.sql.Types.BOOLEAN:
                return random.nextBoolean();
            case java.sql.Types.TINYINT:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.INTEGER:
                return random.nextInt(0, Integer.MAX_VALUE);
            case java.sql.Types.BIGINT:
                return random.nextLong(0, Long.MAX_VALUE);
            case java.sql.Types.FLOAT:
            case java.sql.Types.DOUBLE:
                return random.nextDouble(0d, Double.MAX_VALUE);
            case java.sql.Types.REAL:
                return random.nextFloat();
            case java.sql.Types.NUMERIC:
            case java.sql.Types.DECIMAL:
                return randomBigDecimal();
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.LONGVARCHAR:
                return randomWord(precision);
            case java.sql.Types.DATE:
                return java.sql.Date.valueOf(randomDate());
            case java.sql.Types.TIME:
                return java.sql.Time.valueOf(randomTime());
            case java.sql.Types.TIMESTAMP:
                return java.sql.Timestamp.valueOf(randomDateTime());
            case java.sql.Types.BLOB:
                return randomWord(precision).getBytes(StandardCharsets.UTF_8);
            case java.sql.Types.CLOB:
                return randomLoreIpsum(32, precision, true);
            case java.sql.Types.OTHER:
                return randomJson(1, 2);
            case java.sql.Types.ARRAY:
            case java.sql.Types.STRUCT:
            case java.sql.Types.REF:
            case java.sql.Types.DATALINK:
            case java.sql.Types.ROWID:
            case java.sql.Types.NULL:
            case java.sql.Types.JAVA_OBJECT:
            case java.sql.Types.DISTINCT:
            case java.sql.Types.BINARY:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
            default:
                return null;
        }
    }
}
