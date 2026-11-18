# 05 — Erros e validação

## Formato padrão de erro

Toda resposta de erro (400, 401, 404, 500) tem o mesmo corpo. Nunca devolver stack trace.

```json
{
  "timestamp": "2026-11-12T19:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Dados da requisição inválidos",
  "path": "/v1/user",
  "errors": [
    { "field": "email", "message": "E-mail inválido" },
    { "field": "confirmPassword", "message": "As senhas não conferem" }
  ]
}
```

- `errors` só aparece quando há erros por campo (validação). Nos demais casos é omitido.
- `204 No Content` **nunca** tem corpo: `ResponseEntity.noContent().build()`.

```java
/** Corpo padrão das respostas de erro da API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(Instant timestamp, int status, String error, String message,
                            String path, List<FieldError> errors) {
    public record FieldError(String field, String message) {}
}
```

## Exceções da aplicação

| Classe | Status | Quando lançar |
|---|---|---|
| `NotFoundException` | 404 | Recurso pelo id não existe (`"Usuário 10 não encontrado"`) |
| `BadRequestException` | 400 | Regra de negócio violada: email/slug duplicado, senhas diferentes, categoria inexistente, base64 inválido, query param inválido, credenciais inválidas no login |

Ambas estendem `RuntimeException`. Podem carregar uma lista opcional de `FieldError`.

## `GlobalExceptionHandler` (`@RestControllerAdvice`)

| Exceção capturada | Status | Mensagem |
|---|---|---|
| `NotFoundException` | 404 | a da exceção |
| `BadRequestException` | 400 | a da exceção |
| `MethodArgumentNotValidException` (`@Valid` no body) | 400 | "Dados da requisição inválidos" + `errors` |
| `HandlerMethodValidationException` / `ConstraintViolationException` (params) | 400 | "Parâmetros inválidos" + `errors` |
| `HttpMessageNotReadableException` (JSON malformado, tipo errado) | 400 | "Corpo da requisição inválido ou malformado" |
| `MethodArgumentTypeMismatchException` (`/v1/user/abc`) | 400 | "Parâmetro 'id' inválido" |
| `MissingServletRequestParameterException` | 400 | "Parâmetro 'x' é obrigatório" |
| `HttpMediaTypeNotSupportedException` (sem `Content-Type: application/json`) | 400 | "Content-Type deve ser application/json" |
| `DataIntegrityViolationException` (constraint do banco, fallback) | 400 | "Violação de integridade dos dados" |
| `NoResourceFoundException` (rota inexistente) | 404 | "Recurso não encontrado" |
| `HttpRequestMethodNotSupportedException` | 405 | "Método não suportado" |
| `Exception` (qualquer outra) | 500 | "Erro interno do servidor" — logar a exceção com `log.error` |

> **401** não passa pelo `@RestControllerAdvice`: ele é gerado pelo Spring Security antes de chegar
> ao controller. Por isso o `JsonAuthenticationEntryPoint` (spec 04) escreve o mesmo `ErrorResponse`.

> Verificar duplicidade de email/slug **no service** (`existsByEmail`) antes de salvar, para dar uma
> mensagem clara. O handler de `DataIntegrityViolationException` é só uma rede de segurança.

## Regras gerais de validação

- Strings obrigatórias: `@NotBlank` (rejeita `null`, `""` e `"   "`).
- Fazer `trim()` em textos antes de salvar; email também em `toLowerCase()`.
- Números: `@NotNull`, `@PositiveOrZero`, `@DecimalMin("0.0")`.
- Slug: `@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug deve conter apenas letras minúsculas, números e hífens")`.
- Campos desconhecidos no JSON são **ignorados** (padrão do Spring Boot).
- Path variable `id`: `Integer`; valor não numérico → 400.

## Parâmetros de busca (`limit`, `page`)

Usados em `/v1/category/search` e `/v1/product/search`. Validar em `SearchParams`:

| Parâmetro | Padrão | Válido | Inválido (→ 400) |
|---|---|---|---|
| `limit` | 12 | `-1` (todos) ou inteiro `>= 1` | `0`, `-2`, `abc` |
| `page` | 1 | inteiro `>= 1`; ignorado quando `limit=-1` | `0`, `-1`, `abc` |

Conversão para Spring Data: `limit == -1 ? Pageable.unpaged(sort) : PageRequest.of(page - 1, limit, sort)`,
com `sort = Sort.by("id").ascending()` para paginação estável.

## Critérios de aceite

- [ ] Todo erro devolve `Content-Type: application/json` no formato acima, inclusive 401 e rota inexistente.
- [ ] JSON malformado (`{"name": }`) retorna 400, não 500.
- [ ] `GET /v1/category/abc` retorna 400.
- [ ] Erro inesperado retorna 500 sem stack trace no corpo, e o stack trace aparece no log.
- [ ] Respostas 204 têm corpo vazio.
