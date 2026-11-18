package br.com.digitalstore.api.dto.product;

import br.com.digitalstore.api.model.enums.OptionShape;
import br.com.digitalstore.api.model.enums.OptionType;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record OptionRequest(
        @Schema(example = "1") Integer id,
        @Schema(example = "Tamanho") String title,
        @Schema(example = "square") OptionShape shape,
        @Schema(type = "string", example = "4px", description = "Número ou texto em px") Object radius,
        @Schema(example = "text") OptionType type,
        @Schema(example = "[\"PP\", \"M\", \"GG\"]")
        @JsonAlias("value") List<String> values,
        @Schema(example = "false") Boolean deleted
) {
}
