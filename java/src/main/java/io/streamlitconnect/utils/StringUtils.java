package io.streamlitconnect.utils;

import static org.apache.commons.lang3.Validate.isTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class StringUtils {

    public static final String EMPTY_STRING = "";

    public static final String NEW_LINE = "\n";

    public static final String NULL = "null";

    public static final String COMMA_SPACE_DELIMITER = ", ";

    public static final String COLON_SPACE_DELIMITER = ": ";

    public static final char SPACE_CHAR = ' ';

    public static String randomNumber(int length) {
        isTrue(length > 0, "Length must be greater than 0: %d", length);
        return RandomStringUtils.random(length, false, true);
    }

    public static String truncate(String str, int length) {
        return truncate(str, length, EMPTY_STRING, true);
    }

    public static String truncate(String str, int length, String affix, boolean fromStart) {
        if (str == null) {
            return null;
        }
        isTrue(length >= 0, "Length can not be negative: %d", length);
        isTrue(affix == null || affix.length() <= 64,
            "Length of affix [%d] must not be greater than 64",
            affix.length());

        if (str.length() <= length) {
            return str;
        }
        int suffixLength = affix.length();

        if (!fromStart) {
            // Truncate from end
            if (length <= suffixLength) {
                return affix.substring(0, length);
            } else {
                return str.substring(0, length - suffixLength) + affix;
            }
        } else {
            // Truncate from start
            if (length <= suffixLength) {
                return affix.substring(0, length);
            } else {
                return affix + str.substring(str.length() - (length - suffixLength));
            }
        }
    }

    public static String prettyPrint(Map<?, ?> map) {
        return prettyPrint(map, false, 0, SPACE_CHAR, COLON_SPACE_DELIMITER);
    }

    public static String prettyPrint(Map<?, ?> map, boolean sortKeys) {
        return prettyPrint(map, sortKeys, 0, SPACE_CHAR, COLON_SPACE_DELIMITER);
    }

    public static String prettyPrint(Map<?, ?> map, boolean sortKeys, int offset) {
        return prettyPrint(map, sortKeys, offset, SPACE_CHAR, COLON_SPACE_DELIMITER);
    }

    public static String prettyPrint(Map<?, ?> map, boolean sortKeys, int offset, char paddingChar, String keyValueSeparator) {
        List<Entry<?, ?>> effectiveMap =
            sortKeys ? map.entrySet().stream().sorted(Comparator.comparing(o -> o.getKey().toString())).collect(
                Collectors.toList()) : new ArrayList<>(map.entrySet());

        if (effectiveMap.isEmpty()) {
            return EMPTY_STRING;
        }

        int longestKeyLength = effectiveMap.stream().mapToInt(e -> e.getKey().toString().length()).max().orElse(0);
        String padding = String.format("%" + offset + "s", "").replace(' ', paddingChar);

        return effectiveMap.stream()
            .map(entry -> {
                String keyPadding = String.format("%" + (longestKeyLength - entry.getKey().toString().length()) + "s",
                    EMPTY_STRING);
                String valueString;
                if (entry.getValue() instanceof Map) {
                    valueString = NEW_LINE +
                        prettyPrint((Map<?, ?>) entry.getValue(), sortKeys, offset + 2, paddingChar, keyValueSeparator);
                } else if (entry.getValue() instanceof List) {
                    valueString = NEW_LINE +
                        ((List<?>) entry.getValue()).stream().map(Object::toString)
                            .collect(Collectors.joining(COMMA_SPACE_DELIMITER));
                } else {
                    valueString = (entry.getValue() != null) ? entry.getValue().toString() : NULL;
                }
                return padding + entry.getKey().toString() + keyPadding + keyValueSeparator + valueString;
            })
            .collect(Collectors.joining(NEW_LINE));
    }

    public static String prettyPrint(List<?> list) {
        return prettyPrint(list, 0, SPACE_CHAR);
    }

    public static String prettyPrint(List<?> list, int offset, char paddingChar) {
        String padding = offset > 0 ?
            String.format("%" + offset + "s", "").replace(' ', paddingChar) :
            EMPTY_STRING;

        return list.stream()
            .map(item -> {
                if (item instanceof Map) {
                    return padding +
                        prettyPrint((Map<?, ?>) item, true, offset + 2, paddingChar, COLON_SPACE_DELIMITER);
                } else if (item instanceof List) {
                    return padding + prettyPrint((List<?>) item, offset + 2, paddingChar);
                } else {
                    return padding + item;
                }
            })
            .collect(Collectors.joining(NEW_LINE));
    }

}
