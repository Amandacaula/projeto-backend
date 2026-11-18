package br.com.digitalstore.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @Schema(example = "user@mail.com")
        @NotBlank(message = "O campo email é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(example = "123@123")
        @NotBlank(message = "O campo password é obrigatório")
        String password
) {
}
