# 07 — API de Categorias

Base: `/v1/category` · Controller: `CategoryController` · Service: `CategoryService`

## Modelos

**`CategoryResponse`**

```json
{ "id": 1, "name": "Shoes", "slug": "shoes", "use_in_menu": true }
```

**`CategoryRequest`** (POST e PUT)

| Campo | Regras |
|---|---|
| `name` | obrigatório, máx. 100 |
| `slug` | obrigatório, máx. 100, padrão de slug (spec 05), **único** |
| `use_in_menu` | opcional, boolean; padrão `false` |

---

## `GET /v1/category/search` — listar categorias

Público.

**Query params**

| Param | Exemplo | Padrão | Regra |
|---|---|---|---|
| `limit` | `limit=-1` | 12 | `-1` = todos; senão `>= 1` (spec 05) |
| `page` | `page=1` | 1 | `>= 1`; ignorado quando `limit=-1` |
| `fields` | `fields=name,slug` | todos | Subconjunto de `id,name,slug,use_in_menu`. Campo desconhecido → 400 (D13) |
| `use_in_menu` | `use_in_menu=true` | sem filtro | `true` ou `false`; outro valor → 400 |

Ordenação: `id` crescente.

**Resposta 200**

```json
{
  "data": [
    { "id": 1, "name": "Shoes", "slug": "shoes", "use_in_menu": true },
    { "id": 2, "name": "Offers", "slug": "offers", "use_in_menu": true }
  ],
  "total": 10,
  "limit": 12,
  "page": 1
}
```

- `total` = total de registros que atendem ao filtro (não o tamanho da página).
- `limit` e `page` ecoam os valores efetivos (com `limit=-1`, `page` = 1).
- Página além do fim → 200 com `data: []` e o `total` real.

**Implementação de `fields`:** montar `CategoryResponse`, converter para `Map` com o `ObjectMapper`
(já em snake_case) e manter só as chaves pedidas, na ordem do DTO. Isso fica em
`util/FieldSelector`, reaproveitado pela busca de produtos.

**Resposta genérica:** `PageResponse<T>(List<T> data, long total, int limit, int page)`.

| Status | Quando |
|---|---|
| 200 | Sucesso (inclusive lista vazia) |
| 400 | `limit`, `page`, `fields` ou `use_in_menu` inválidos |

---

## `GET /v1/category/{id}` — obter categoria

Público.

| Status | Quando | Corpo |
|---|---|---|
| 200 | Existe | `CategoryResponse` |
| 400 | `id` não numérico | `ErrorResponse` |
| 404 | Não existe | `ErrorResponse` |

> A rota literal `/search` tem prioridade sobre `/{id}` no Spring MVC; não há conflito.

---

## `POST /v1/category` — criar categoria

**Requer token.** Header `Content-Type: application/json`.

```json
{ "name": "Shoes", "slug": "shoes", "use_in_menu": true }
```

| Status | Quando | Corpo |
|---|---|---|
| 201 | Criada | `CategoryResponse` + `Location: /v1/category/{id}` |
| 400 | Validação ou slug duplicado (`"Slug já cadastrado"`) | `ErrorResponse` |
| 401 | Sem token / token inválido | `ErrorResponse` |

---

## `PUT /v1/category/{id}` — atualizar categoria

**Requer token.** Mesmo payload e validações do POST. Slug duplicado só conta se pertencer a **outra** categoria.

| Status | Quando |
|---|---|
| 204 | Atualizada |
| 400 | Validação ou slug duplicado |
| 401 | Sem token / token inválido |
| 404 | Não existe |

---

## `DELETE /v1/category/{id}` — excluir categoria

**Requer token.** Remove a categoria e os vínculos em `product_categories` (`ON DELETE CASCADE`);
os produtos continuam existindo.

| Status | Quando |
|---|---|
| 204 | Excluída |
| 401 | Sem token / token inválido |
| 404 | Não existe |

---

## Critérios de aceite

- [ ] `GET /v1/category/search` sem params → até 12 itens, `limit: 12`, `page: 1`.
- [ ] Com 15 categorias: `limit=10&page=2` → 5 itens, `total: 15`.
- [ ] `limit=-1&page=3` → todas as categorias, `page: 1`.
- [ ] `fields=name,slug` → itens só com `name` e `slug`.
- [ ] `fields=foo` → 400; `limit=0` → 400; `page=0` → 400; `use_in_menu=talvez` → 400.
- [ ] `use_in_menu=true` → só categorias com `use_in_menu: true`, e `total` considera o filtro.
- [ ] `POST` sem token → 401; slug repetido → 400; `slug: "Tênis Novo"` → 400; válido → 201.
- [ ] `use_in_menu` omitido no POST → categoria criada com `false`.
- [ ] `PUT`/`DELETE` em id inexistente (com token) → 404.
- [ ] Excluir categoria vinculada a produto → 204 e o produto deixa de listar o id em `category_ids`.
