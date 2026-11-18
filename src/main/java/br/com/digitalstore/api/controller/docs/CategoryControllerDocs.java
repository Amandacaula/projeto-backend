package br.com.digitalstore.api.controller.docs;

import br.com.digitalstore.api.config.OpenApiConfig;
import br.com.digitalstore.api.dto.category.CategoryRequest;
import br.com.digitalstore.api.dto.category.CategoryResponse;
import br.com.digitalstore.api.dto.common.ErrorResponse;
import br.com.digitalstore.api.dto.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Tag(name = "Categorias")
public interface CategoryControllerDocs {

    @Operation(summary = "Lista categorias com paginação, seleção de campos e filtro de menu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    PageResponse<Map<String, Object>> search(
            @Parameter(description = "Itens por página; -1 retorna todos", example = "12") Integer limit,
            @Parameter(description = "Página (ignorada quando limit=-1)", example = "1") Integer page,
            @Parameter(description = "Campos retornados: id,name,slug,use_in_menu", example = "name,slug") String fields,
            @Parameter(description = "Filtra categorias exibidas no menu", example = "true") Boolean useInMenu);

    @Operation(summary = "Obtém uma categoria pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada"),
            @ApiResponse(responseCode = "400", description = "Id inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    CategoryResponse findById(Integer id);

    @Operation(summary = "Cria uma categoria", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou slug já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<CategoryResponse> create(CategoryRequest request);

    @Operation(summary = "Atualiza uma categoria", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou slug já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> update(Integer id, CategoryRequest request);

    @Operation(summary = "Exclui uma categoria", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria excluída"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> delete(Integer id);
}
