package br.com.digitalstore.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String baseUrl,
        @NotBlank String uploadDir,
        @Valid @NotNull Jwt jwt
) {

    public record Jwt(
            @NotBlank(message = "Defina JWT_SECRET")
            @Size(min = 32, message = "JWT_SECRET deve ter ao menos 32 caracteres")
            String secret,
            @Positive long expirationMinutes
    ) {
    }
}
