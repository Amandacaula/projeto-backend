package br.com.digitalstore.api.dto.product;

import br.com.digitalstore.api.model.enums.OptionShape;
import br.com.digitalstore.api.model.enums.OptionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record ProductResponse(
        Integer id,
        Boolean enabled,
        String name,
        String slug,
        Boolean useInMenu,
        Integer stock,
        String description,
        BigDecimal price,
        BigDecimal priceWithDiscount,
        List<Integer> categoryIds,
        List<Image> images,
        List<Option> options
) {

    public static final Set<String> FIELDS = Set.of("id", "enabled", "name", "slug", "use_in_menu", "stock",
            "description", "price", "price_with_discount", "category_ids", "images", "options");

    public record Image(Integer id, String content) {
    }

    public record Option(Integer id, String title, OptionShape shape, Integer radius, OptionType type,
                         List<String> values) {
    }
}
