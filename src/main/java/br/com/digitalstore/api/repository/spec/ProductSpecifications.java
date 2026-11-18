package br.com.digitalstore.api.repository.spec;

import br.com.digitalstore.api.model.Category;
import br.com.digitalstore.api.model.Product;
import br.com.digitalstore.api.model.ProductOption;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private ProductSpecifications() {
    }

    public static Specification<Product> from(ProductSearchCriteria criteria) {
        List<Specification<Product>> specs = new ArrayList<>();
        if (criteria.match() != null) {
            specs.add(matches(criteria.match()));
        }
        if (!criteria.categoryIds().isEmpty()) {
            specs.add(inAnyCategory(criteria.categoryIds()));
        }
        if (criteria.minPrice() != null) {
            specs.add(priceBetween(criteria.minPrice(), criteria.maxPrice()));
        }
        criteria.options().forEach((optionId, values) -> specs.add(hasOptionValue(optionId, values)));
        return Specification.allOf(specs);
    }

    static Specification<Product> matches(String term) {
        String pattern = "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, ESCAPE),
                cb.like(cb.lower(root.get("description")), pattern, ESCAPE));
    }

    static Specification<Product> inAnyCategory(List<Integer> categoryIds) {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<Product> p = sub.from(Product.class);
            Join<Product, Category> category = p.join("categories");
            sub.select(p.get("id"))
                    .where(cb.equal(p.get("id"), root.get("id")), category.get("id").in(categoryIds));
            return cb.exists(sub);
        };
    }

    static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> cb.between(root.get("price"), min, max);
    }

    static Specification<Product> hasOptionValue(Integer optionId, List<String> values) {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<ProductOption> option = sub.from(ProductOption.class);
            Expression<String> wrapped = cb.concat(cb.concat(",", option.get("values")), ",");
            Predicate anyValue = cb.or(values.stream()
                    .map(v -> cb.like(wrapped, "%," + escapeLike(v) + ",%", ESCAPE))
                    .toArray(Predicate[]::new));
            sub.select(option.get("id"))
                    .where(cb.equal(option.get("product"), root), cb.equal(option.get("id"), optionId), anyValue);
            return cb.exists(sub);
        };
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
