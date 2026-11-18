package br.com.digitalstore.api.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OptionShape {
    SQUARE, CIRCLE;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static OptionShape fromValue(String value) {
        return Arrays.stream(values())
                .filter(v -> v.value().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Valor inválido para shape: " + value));
    }
}
