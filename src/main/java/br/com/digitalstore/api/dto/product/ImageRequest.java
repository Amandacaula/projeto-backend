package br.com.digitalstore.api.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageRequest(
        @Schema(example = "2") Integer id,
        @Schema(example = "image/png") String type,
        @Schema(example = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==")
        String content,
        @Schema(example = "false") Boolean deleted
) {
}
