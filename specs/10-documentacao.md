# 10 — Documentação

O critério de avaliação pede **documentação da API (Swagger)** e **guia de setup no README**.

## Swagger (springdoc-openapi)

Dependência na spec 01. URLs:

| URL | Conteúdo |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interface interativa |
| `http://localhost:8080/v3/api-docs` | Especificação OpenAPI 3 em JSON |

### `OpenApiConfig`

```java
/** Metadados da API e esquema de autenticação Bearer usado pelo botão "Authorize". */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Digital Store API",
        version = "1.0",
        description = "API REST de e-commerce: usuários, categorias e produtos."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
                scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {}
```

### Anotações nos controllers

```java
@Tag(name = "Categorias")
@RestController
@RequestMapping("/v1/category")
public class CategoryController {

    @Operation(summary = "Cria uma categoria", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Categoria criada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) { ... }
}
```

Regras:

- Todo endpoint tem `@Operation(summary = ...)` e `@ApiResponses` com **todos** os status das specs 04, 06–08.
- Endpoints protegidos têm `security = @SecurityRequirement(name = "bearerAuth")`; os públicos não.
- Query params da busca documentados com `@Parameter(description = ..., example = ...)`.
  Para `option[{id}]`, descrever o formato no `description` do `@Operation` (não dá para tipar a chave dinâmica).
- DTOs com `@Schema(example = "...")` nos campos, para o Swagger mostrar payloads realistas.

## README.md da raiz (guia de setup)

Estrutura sugerida:

```markdown
# Digital Store API — Projeto Back-end

API REST de e-commerce em Java 21 + Spring Boot, PostgreSQL e JWT.

## Equipe
- Nome 1
- Nome 2
- Nome 3

## Tecnologias
(tabela de equivalência Node → Java da spec 00)

## Pré-requisitos
- Docker Desktop
- (opcional, para desenvolver) JDK 21

## Como rodar
1. Clonar o repositório
2. `cp .env.example .env`
3. `docker compose up --build`
4. Acessar http://localhost:8080/swagger-ui.html

## Como usar a API
1. `POST /v1/user` para criar um usuário
2. `POST /v1/user/token` para obter o JWT
3. No Swagger, clicar em **Authorize** e colar o token
4. Exemplos de requisição (curl) para cada recurso

## Variáveis de ambiente
(tabela com cada variável do .env.example e o que faz)

## Desenvolvimento
- Banco no Docker e API local com hot reload
- Rodar testes: `./mvnw test`
- Resetar banco: `docker compose down -v`

## Estrutura do projeto
(árvore de pastas da spec 01 + explicação do MVC simplificado)

## Banco de dados
(diagrama ER da spec 03 + lista de migrations)

## Decisões de projeto
(tabela D1–D19 da spec 00 — mostra que as inconsistências do enunciado foram tratadas de propósito)
```

## Critérios de aceite

- [ ] Swagger UI lista os 15 endpoints agrupados em Usuários, Autenticação, Categorias e Produtos.
- [ ] Endpoints protegidos mostram o cadeado; após **Authorize**, as chamadas funcionam pelo próprio Swagger.
- [ ] Alguém que nunca viu o projeto consegue rodá-lo seguindo só o README.
- [ ] O README lista os nomes da equipe (exigência da entrega).
