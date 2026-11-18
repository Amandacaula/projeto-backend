package br.com.digitalstore.api.repository.spec;

import br.com.digitalstore.api.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ProductSearchCriteria(String match, List<Integer> categoryIds, BigDecimal minPrice,
                                    BigDecimal maxPrice, Map<Integer, List<String>> options) {

    private static final Pattern PRICE_RANGE = Pattern.compile("^(\\d+(?:\\.\\d{1,2})?)-(\\d+(?:\\.\\d{1,2})?)$");
    private static final Pattern OPTION_KEY = Pattern.compile("^option\\[(.*)]$");

    public static ProductSearchCriteria parse(String match, String categoryIds, String priceRange,
                                              Map<String, String> allParams) {
        String term = match == null || match.isBlank() ? null : match.trim();

        BigDecimal min = null;
        BigDecimal max = null;
        if (priceRange != null && !priceRange.isBlank()) {
            Matcher m = PRICE_RANGE.matcher(priceRange.trim());
            if (!m.matches()) {
                throw BadRequestException.forField("price-range", "price-range deve estar no formato min-max (ex.: 100-200)");
            }
            min = new BigDecimal(m.group(1));
            max = new BigDecimal(m.group(2));
            if (min.compareTo(max) > 0) {
                throw BadRequestException.forField("price-range", "O valor mínimo de price-range não pode ser maior que o máximo");
            }
        }

        return new ProductSearchCriteria(term, parseIds(categoryIds), min, max, parseOptions(allParams));
    }

    private static List<Integer> parseIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .distinct()
                    .toList();
        } catch (NumberFormatException e) {
            throw BadRequestException.forField("category_ids", "category_ids deve conter ids numéricos separados por vírgula");
        }
    }

    private static Map<Integer, List<String>> parseOptions(Map<String, String> allParams) {
        Map<Integer, List<String>> options = new LinkedHashMap<>();
        if (allParams == null) {
            return options;
        }
        allParams.forEach((key, value) -> {
            Matcher m = OPTION_KEY.matcher(key);
            if (!m.matches()) {
                return;
            }
            Integer optionId;
            try {
                optionId = Integer.valueOf(m.group(1).trim());
            } catch (NumberFormatException e) {
                throw BadRequestException.forField(key, "O id da opção em " + key + " deve ser numérico");
            }
            List<String> values = value == null ? List.of() : Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (values.isEmpty()) {
                throw BadRequestException.forField(key, "Informe ao menos um valor em " + key);
            }
            options.put(optionId, values);
        });
        return options;
    }
}
