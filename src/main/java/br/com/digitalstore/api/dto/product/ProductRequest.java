package br.com.digitalstore.api.dto.product;

import br.com.digitalstore.api.dto.common.ValidationPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @Schema(example = "true")
        Boolean enabled,

        @Schema(example = "Produto 01")
        @NotBlank(message = "O campo name é obrigatório")
        @Size(max = 150, message = "O campo name deve ter no máximo 150 caracteres")
        String name,

        @Schema(example = "produto-01")
        @NotBlank(message = "O campo slug é obrigatório")
        @Size(max = 150, message = "O campo slug deve ter no máximo 150 caracteres")
        @Pattern(regexp = ValidationPatterns.SLUG, message = ValidationPatterns.SLUG_MESSAGE)
        String slug,

        @Schema(example = "false")
        Boolean useInMenu,

        @Schema(example = "10")
        @PositiveOrZero(message = "O campo stock não pode ser negativo")
        Integer stock,

        @Schema(example = "Descrição do produto 01")
        @Size(max = 1000, message = "O campo description deve ter no máximo 1000 caracteres")
        String description,

        @Schema(example = "119.90")
        @NotNull(message = "O campo price é obrigatório")
        @DecimalMin(value = "0.0", message = "O campo price não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "O campo price aceita até 2 casas decimais")
        BigDecimal price,

        @Schema(example = "99.90")
        @NotNull(message = "O campo price_with_discount é obrigatório")
        @DecimalMin(value = "0.0", message = "O campo price_with_discount não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "O campo price_with_discount aceita até 2 casas decimais")
        BigDecimal priceWithDiscount,

        @Schema(example = "[1, 2]")
        List<@NotNull(message = "category_ids não pode conter valores nulos") Integer> categoryIds,

        List<@NotNull(message = "images não pode conter itens nulos") ImageRequest> images,

        List<@NotNull(message = "options não pode conter itens nulos") OptionRequest> options
) {
}
