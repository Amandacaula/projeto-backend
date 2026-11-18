package br.com.digitalstore.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Schema(example = "user firstname")
        @NotBlank(message = "O campo firstname é obrigatório")
        @Size(max = 100, message = "O campo firstname deve ter no máximo 100 caracteres")
        String firstname,

        @Schema(example = "user surname")
        @NotBlank(message = "O campo surname é obrigatório")
        @Size(max = 100, message = "O campo surname deve ter no máximo 100 caracteres")
        String surname,

        @Schema(example = "user@mail.com")
        @NotBlank(message = "O campo email é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O campo email deve ter no máximo 150 caracteres")
        String email
) {
}
