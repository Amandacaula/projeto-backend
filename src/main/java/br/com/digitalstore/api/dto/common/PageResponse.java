package br.com.digitalstore.api.dto.common;

import java.util.List;

public record PageResponse<T>(List<T> data, long total, int limit, int page) {
}
