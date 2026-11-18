package br.com.digitalstore.api.controller.docs;

import br.com.digitalstore.api.config.OpenApiConfig;
import br.com.digitalstore.api.dto.common.ErrorResponse;
import br.com.digitalstore.api.dto.user.UserCreateRequest;
import br.com.digitalstore.api.dto.user.UserResponse;
import br.com.digitalstore.api.dto.user.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Usuários")
public interface UserControllerDocs {

    @Operation(summary = "Obtém um usuário pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "400", description = "Id inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    UserResponse findById(Integer id);

    @Operation(summary = "Cadastra um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, senhas diferentes ou email já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<UserResponse> create(UserCreateRequest request);

    @Operation(summary = "Atualiza um usuário", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> update(Integer id, UserUpdateRequest request);

    @Operation(summary = "Exclui um usuário", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário excluído"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> delete(Integer id);
}
