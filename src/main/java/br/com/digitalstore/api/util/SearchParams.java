package br.com.digitalstore.api.util;

import br.com.digitalstore.api.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record SearchParams(int limit, int page) {

    public static final int DEFAULT_LIMIT = 12;
    public static final int DEFAULT_PAGE = 1;
    public static final int UNLIMITED = -1;

    public static SearchParams of(Integer limit, Integer page) {
        int effectiveLimit = limit == null ? DEFAULT_LIMIT : limit;
        int effectivePage = page == null ? DEFAULT_PAGE : page;

        if (effectiveLimit != UNLIMITED && effectiveLimit < 1) {
            throw BadRequestException.forField("limit", "limit deve ser -1 (todos) ou um inteiro maior que zero");
        }
        if (effectivePage < 1) {
            throw BadRequestException.forField("page", "page deve ser um inteiro maior que zero");
        }
        return new SearchParams(effectiveLimit, effectiveLimit == UNLIMITED ? DEFAULT_PAGE : effectivePage);
    }

    public boolean isUnlimited() {
        return limit == UNLIMITED;
    }

    public Pageable toPageable(Sort sort) {
        return isUnlimited() ? Pageable.unpaged(sort) : PageRequest.of(page - 1, limit, sort);
    }
}
