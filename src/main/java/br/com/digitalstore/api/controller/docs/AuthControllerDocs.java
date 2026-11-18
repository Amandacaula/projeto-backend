package br.com.digitalstore.api.controller.docs;

import br.com.digitalstore.api.dto.common.ErrorResponse;
import br.com.digitalstore.api.dto.user.TokenRequest;
import br.com.digitalstore.api.dto.user.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticação")
public interface AuthControllerDocs {

    @Operation(summary = "Gera um token JWT a partir de email e senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token gerado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou credenciais incorretas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    TokenResponse token(TokenRequest request);
}
