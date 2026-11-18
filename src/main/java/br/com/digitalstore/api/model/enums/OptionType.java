package br.com.digitalstore.api.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OptionType {
    TEXT, COLOR;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static OptionType fromValue(String value) {
        return Arrays.stream(values())
                .filter(v -> v.value().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Valor inválido para type: " + value));
    }
}
