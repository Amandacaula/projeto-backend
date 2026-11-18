package br.com.digitalstore.api.dto.category;

import br.com.digitalstore.api.model.Category;

import java.util.Set;

public record CategoryResponse(Integer id, String name, String slug, Boolean useInMenu) {

    public static final Set<String> FIELDS = Set.of("id", "name", "slug", "use_in_menu");

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getUseInMenu());
    }
}
