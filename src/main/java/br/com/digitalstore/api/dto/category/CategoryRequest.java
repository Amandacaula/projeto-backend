package br.com.digitalstore.api.dto.category;

import br.com.digitalstore.api.dto.common.ValidationPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @Schema(example = "Shoes")
        @NotBlank(message = "O campo name é obrigatório")
        @Size(max = 100, message = "O campo name deve ter no máximo 100 caracteres")
        String name,

        @Schema(example = "shoes")
        @NotBlank(message = "O campo slug é obrigatório")
        @Size(max = 100, message = "O campo slug deve ter no máximo 100 caracteres")
        @Pattern(regexp = ValidationPatterns.SLUG, message = ValidationPatterns.SLUG_MESSAGE)
        String slug,

        @Schema(example = "true")
        Boolean useInMenu
) {
}
