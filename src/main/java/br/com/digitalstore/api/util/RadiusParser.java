package br.com.digitalstore.api.util;

import br.com.digitalstore.api.exception.BadRequestException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RadiusParser {

    private static final Pattern TEXT = Pattern.compile("^\\s*(\\d{1,4})\\s*(px)?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final int MAX = 9999;

    private RadiusParser() {
    }

    public static int parse(Object value, String field) {
        if (value instanceof Number number) {
            double d = number.doubleValue();
            if (d % 1 != 0 || d < 0 || d > MAX) {
                throw invalid(field);
            }
            return number.intValue();
        }
        if (value instanceof String text) {
            Matcher matcher = TEXT.matcher(text);
            if (matcher.matches()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        throw invalid(field);
    }

    private static BadRequestException invalid(String field) {
        return BadRequestException.forField(field, "radius deve ser um inteiro entre 0 e " + MAX + " (ex.: 4 ou \"4px\")");
    }
}
