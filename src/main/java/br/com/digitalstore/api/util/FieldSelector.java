package br.com.digitalstore.api.util;

import br.com.digitalstore.api.exception.BadRequestException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FieldSelector {

    private final ObjectMapper objectMapper;

    public FieldSelector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Set<String> parse(String fields, Set<String> allowed) {
        if (fields == null || fields.isBlank()) {
            return null;
        }
        Set<String> requested = new LinkedHashSet<>();
        Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .forEach(requested::add);

        List<String> invalid = requested.stream().filter(f -> !allowed.contains(f)).toList();
        if (!invalid.isEmpty()) {
            throw BadRequestException.forField("fields",
                    "Campos inválidos em fields: " + invalid + ". Permitidos: " + allowed);
        }
        return requested.isEmpty() ? null : requested;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> select(List<?> items, Set<String> fields) {
        return items.stream()
                .map(item -> {
                    Map<String, Object> map = objectMapper.convertValue(item, Map.class);
                    if (fields != null) {
                        map.keySet().retainAll(fields);
                    }
                    return map;
                })
                .toList();
    }
}
