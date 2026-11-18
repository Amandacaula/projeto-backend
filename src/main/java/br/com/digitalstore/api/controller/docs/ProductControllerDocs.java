package br.com.digitalstore.api.controller.docs;

import br.com.digitalstore.api.config.OpenApiConfig;
import br.com.digitalstore.api.dto.common.ErrorResponse;
import br.com.digitalstore.api.dto.common.PageResponse;
import br.com.digitalstore.api.dto.product.ProductRequest;
import br.com.digitalstore.api.dto.product.ProductResponse;
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

@Tag(name = "Produtos")
public interface ProductControllerDocs {

    @Operation(summary = "Busca produtos com paginação, seleção de campos e filtros",
            description = """
                    Todos os filtros são combinados com E.
                    Filtro por opção: `option[{id}]=GG,PP` retorna produtos cuja opção de id `{id}` \
                    contém algum dos valores (ex.: `option[45]=GG,PP`).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    PageResponse<Map<String, Object>> search(
            @Parameter(description = "Itens por página; -1 retorna todos", example = "12") Integer limit,
            @Parameter(description = "Página (ignorada quando limit=-1)", example = "1") Integer page,
            @Parameter(description = "Campos retornados", example = "name,images,price") String fields,
            @Parameter(description = "Termo buscado no nome ou na descrição", example = "Tênis") String match,
            @Parameter(description = "Ids de categoria (produto em qualquer uma delas)", example = "15,24") String categoryIds,
            @Parameter(description = "Faixa de preço inclusiva min-max", example = "100-200") String priceRange,
            @Parameter(hidden = true) Map<String, String> allParams);

    @Operation(summary = "Obtém um produto pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "400", description = "Id inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ProductResponse findById(Integer id);

    @Operation(summary = "Cria um produto", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ProductResponse> create(ProductRequest request);

    @Operation(summary = "Atualiza um produto", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH),
            description = """
                    Imagens: sem id cria; id + deleted remove; id + base64 substitui; id + URL mantém.
                    Opções: sem id cria; id + deleted remove; id + campos atualiza só os campos enviados.
                    category_ids, quando enviado, substitui os vínculos.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados ou operações inválidas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> update(Integer id, ProductRequest request);

    @Operation(summary = "Exclui um produto", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto excluído"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> delete(Integer id);
}
