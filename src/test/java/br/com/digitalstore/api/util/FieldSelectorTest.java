package br.com.digitalstore.api.util;

import br.com.digitalstore.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldSelectorTest {

    record Item(Integer id, String name, Boolean useInMenu) {
    }

    private static final Set<String> ALLOWED = Set.of("id", "name", "use_in_menu");

    private final FieldSelector selector = new FieldSelector(
            JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build());

    @Test
    void semFieldsRetornaTodosOsCamposEmSnakeCase() {
        List<Map<String, Object>> result = selector.select(List.of(new Item(1, "Shoes", true)),
                selector.parse(null, ALLOWED));
        assertThat(result.getFirst()).containsOnlyKeys("id", "name", "use_in_menu");
    }

    @Test
    void mantemSoOsCamposPedidos() {
        Set<String> fields = selector.parse(" name , use_in_menu ", ALLOWED);
        List<Map<String, Object>> result = selector.select(List.of(new Item(1, "Shoes", true)), fields);
        assertThat(result.getFirst()).containsOnlyKeys("name", "use_in_menu");
    }

    @Test
    void rejeitaCampoDesconhecido() {
        assertThatThrownBy(() -> selector.parse("name,foo", ALLOWED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("foo");
    }
}
