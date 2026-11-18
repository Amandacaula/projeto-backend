# 08 — API de Produtos

Base: `/v1/product` · Controller: `ProductController` · Services: `ProductService`, `ImageStorageService`

## Modelos

**`ProductResponse`** (GET por id, itens da busca, corpo do 201)

```json
{
  "id": 1,
  "enabled": true,
  "name": "Produto 01",
  "slug": "produto-01",
  "use_in_menu": false,
  "stock": 10,
  "description": "Descrição do produto 01",
  "price": 119.90,
  "price_with_discount": 99.90,
  "category_ids": [1, 15, 24, 68],
  "images": [
    { "id": 1, "content": "http://localhost:8080/media/products/1/3f2a9c.png" }
  ],
  "options": [
    { "id": 1, "title": "Tamanho", "shape": "square", "radius": 4, "type": "text", "values": ["PP", "M", "GG"] },
    { "id": 2, "title": "Cor", "shape": "circle", "radius": 0, "type": "color", "values": ["#000", "#333"] }
  ]
}
```

- `images[].content` = `app.base-url` + `/media/` + `path` gravado no banco.
- `options[].values` = coluna `values` separada por vírgula → array.
- `category_ids` em ordem crescente.

**Campos permitidos em `fields`:** `id, enabled, name, slug, use_in_menu, stock, description, price,
price_with_discount, category_ids, images, options`.

---

## `GET /v1/product/search` — buscar produtos

Público.

### Query params

| Param | Exemplo | Padrão | Regra / semântica | Inválido → 400 |
|---|---|---|---|---|
| `limit` | `limit=30` | 12 | `-1` = todos; senão `>= 1` | `0`, `-5`, `abc` |
| `page` | `page=2` | 1 | `>= 1`; ignorado com `limit=-1` | `0`, `abc` |
| `fields` | `fields=name,images,price` | todos | Subconjunto dos campos permitidos | campo desconhecido |
| `match` | `match=Tênis` | — | `name` **ou** `description` contém o termo, sem diferenciar maiúsculas (`ILIKE '%termo%'`). Escapar `%` e `_` do termo | — |
| `category_ids` | `category_ids=15,24` | — | Produto vinculado a **pelo menos uma** das categorias | id não numérico |
| `price-range` | `price-range=100-200` | — | `100 <= price <= 200` (D15) | formato ≠ `min-max`, `min > max` |
| `option[{id}]` | `option[45]=GG,PP` | — | A opção de id 45 do produto contém **algum** dos valores. Vários `option[...]` combinam com **E** | id não numérico, lista vazia |

Todos os filtros combinam com **E**. Ordenação por `id` crescente.

### Leitura dos parâmetros no controller

`price-range` e `option[45]` não viram nomes de variável Java; ler assim:

```java
@GetMapping("/search")
public PageResponse<Map<String, Object>> search(
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) String fields,
        @RequestParam(required = false) String match,
        @RequestParam(name = "category_ids", required = false) String categoryIds,
        @RequestParam(name = "price-range", required = false) String priceRange,
        @RequestParam Map<String, String> allParams) {   // filtra chaves "option\[(\d+)\]"
    ...
}
```

> O Tomcat rejeita `[` e `]` não codificados na query. Por isso `server.tomcat.relaxed-query-chars: "[,]"`
> (spec 02). Clientes que codificam (`option%5B45%5D=GG`) também funcionam.

### Implementação dos filtros (`ProductSpecifications`)

`ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product>`.
Cada filtro é uma `Specification<Product>`; combinar com `Specification.allOf(...)`.

| Filtro | SQL equivalente |
|---|---|
| `match` | `lower(p.name) LIKE :t OR lower(p.description) LIKE :t` |
| `category_ids` | `EXISTS (SELECT 1 FROM product_categories pc WHERE pc.product_id = p.id AND pc.category_id IN (:ids))` |
| `price-range` | `p.price BETWEEN :min AND :max` |
| `option[45]=GG,PP` | `EXISTS (SELECT 1 FROM product_options o WHERE o.product_id = p.id AND o.id = 45 AND (',' \|\| o."values" \|\| ',' LIKE '%,GG,%' OR ',' \|\| o."values" \|\| ',' LIKE '%,PP,%'))` |

- Usar `EXISTS` (subquery) e não `JOIN`, para não duplicar produtos nem distorcer o `total` (spec 03).
- O truque das vírgulas nas pontas evita que `M` case com `GG,MM`. Funciona porque os valores são
  gravados com `trim()` e unidos por `,` sem espaços.

### Resposta 200

```json
{
  "data": [ { "id": 1, "name": "Produto 01", "price": 119.90, "images": [ ... ] } ],
  "total": 120,
  "limit": 12,
  "page": 1
}
```

Mesmas regras de `total`/`limit`/`page` da busca de categorias (spec 07).

---

## `GET /v1/product/{id}` — obter produto

Público. Retorna o produto mesmo com `enabled = false`.

| Status | Quando | Corpo |
|---|---|---|
| 200 | Existe | `ProductResponse` |
| 400 | `id` não numérico | `ErrorResponse` |
| 404 | Não existe | `ErrorResponse` |

---

## `POST /v1/product` — criar produto

**Requer token.** Header `Content-Type: application/json`.

**Payload**

```json
{
  "enabled": true,
  "name": "Produto 01",
  "slug": "produto-01",
  "stock": 10,
  "description": "Descrição do produto 01",
  "price": 119.90,
  "price_with_discount": 99.90,
  "category_ids": [1, 15],
  "images": [
    { "type": "image/png", "content": "iVBORw0KGgoAAAANSUhEUgAA..." }
  ],
  "options": [
    { "title": "Tamanho", "shape": "square", "radius": "4px", "type": "text", "values": ["PP", "GG", "M"] },
    { "title": "Cor", "shape": "circle", "type": "color", "value": ["#000", "#333"] }
  ]
}
```

### Validação

| Campo | Regras |
|---|---|
| `name` | obrigatório, máx. 150 |
| `slug` | obrigatório, padrão de slug, máx. 150, **único** |
| `price` | obrigatório, `>= 0`, até 2 casas decimais |
| `price_with_discount` | obrigatório, `>= 0`, `<= price` |
| `enabled` | opcional, padrão `false` |
| `use_in_menu` | opcional, padrão `false` |
| `stock` | opcional, inteiro `>= 0`, padrão `0` |
| `description` | opcional, máx. 1000 |
| `category_ids` | opcional; todos os ids precisam existir → senão 400 `"Categorias não encontradas: [99]"` |
| `images[].type` | obrigatório; `image/png`, `image/jpeg`, `image/jpg`, `image/webp` ou `image/gif` |
| `images[].content` | obrigatório; base64 válido (com ou sem prefixo `data:image/png;base64,`); máx. 5 MB decodificado |
| `options[].title` | obrigatório, máx. 100 |
| `options[].values` (ou `value`, D8) | obrigatório, array não vazio; nenhum item vazio ou com vírgula (D18); string final ≤ 255 |
| `options[].shape` | opcional, `square` \| `circle`, padrão `square` |
| `options[].type` | opcional, `text` \| `color`, padrão `text` |
| `options[].radius` | opcional; `4`, `"4"` ou `"4px"` → `4` (D9); `>= 0`; padrão `0` |

DTOs: `@Valid` em listas aninhadas (`List<@Valid ImageRequest> images`) para validar cada item.
`radius` pode ser recebido como `JsonNode`/`String` e convertido no service, ou com um
`JsonDeserializer` próprio.

### Comportamento (um único `@Transactional`)

1. Valida slug único e categorias existentes.
2. Salva o produto (para obter o `id`).
3. Para cada imagem: `ImageStorageService.save(productId, type, base64)` → grava o arquivo e
   cria `ProductImage(path, enabled = true)` (D12).
4. Cria as opções (`values` = `String.join(",", valoresComTrim)`).
5. Retorna `ProductResponse`.

| Status | Quando | Corpo |
|---|---|---|
| 201 | Criado | `ProductResponse` + `Location: /v1/product/{id}` |
| 400 | Validação, slug duplicado, categoria inexistente, base64 inválido | `ErrorResponse` |
| 401 | Sem token / token inválido | `ErrorResponse` |

---

## `PUT /v1/product/{id}` — atualizar produto

**Requer token.** Header `Content-Type: application/json`.

**Payload** (exemplo do enunciado)

```json
{
  "enabled": true,
  "name": "Produto 01 atualizado",
  "slug": "produto-01-atualizado",
  "stock": 20,
  "description": "Descrição do produto 01 atualizado",
  "price": 49.9,
  "price_with_discount": 0,
  "category_ids": [1, 15, 24, 68],
  "images": [
    { "type": "image/png", "content": "base64 da imagem nova" },
    { "id": 2, "deleted": true },
    { "id": 3, "content": "base64 que substitui a imagem 3" },
    { "id": 1, "content": "http://localhost:8080/media/products/1/abc.jpg" }
  ],
  "options": [
    { "id": 1, "deleted": true },
    { "id": 2, "radius": "10px", "value": ["42/43", "44/45"] },
    { "title": "Tipo", "shape": "square", "type": "text", "values": ["100% algodão", "65% algodão"] }
  ]
}
```

### Campos simples

Mesmas regras do POST para `name`, `slug`, `price`, `price_with_discount` (obrigatórios) e para os
opcionais. Slug duplicado só conta se for de **outro** produto. Opcionais omitidos (`enabled`,
`use_in_menu`, `stock`, `description`) **mantêm o valor atual**.

> No **POST**, itens de `images`/`options` com `id` ou `deleted` são rejeitados com 400 — essas
> chaves só fazem sentido na atualização.

### `category_ids`

| Envio | Efeito |
|---|---|
| ausente (`null`) | vínculos não mudam |
| `[]` | remove todos os vínculos |
| `[1, 15]` | **substitui** os vínculos por exatamente esses (todos devem existir) |

### `images` — cada item é uma operação

| Formato do item | Operação |
|---|---|
| sem `id`, com `type` + `content` base64 | **cria** imagem nova |
| `id` + `"deleted": true` | **remove** a imagem (linha e arquivo) |
| `id` + `content` base64 | **substitui** o arquivo; `type` opcional (sem `type`, o formato é detectado pelo conteúdo) |
| `id` + `content` começando com `http://` ou `https://` | **mantém** como está (é a URL atual) |
| `id` sem `content` nem `deleted` | **mantém** como está |
| sem `id` e sem `content` / `id` de imagem de outro produto | 400 |

Imagens existentes que não aparecem na lista **são mantidas**. `images` ausente = nada muda.

### `options` — cada item é uma operação

| Formato do item | Operação |
|---|---|
| sem `id` | **cria** (exige `title` e `values`, como no POST) |
| `id` + `"deleted": true` | **remove** |
| `id` + outros campos | **atualiza só os campos enviados** (ex.: só `radius` e `values`) |
| `id` de opção de outro produto | 400 |

Opções não citadas são mantidas. `options` ausente = nada muda.

| Status | Quando |
|---|---|
| 204 | Atualizado |
| 400 | Validação, slug duplicado, categoria/imagem/opção inválida |
| 401 | Sem token / token inválido |
| 404 | Produto não existe |

---

## `DELETE /v1/product/{id}` — excluir produto

**Requer token.** Remove o produto; o banco remove imagens, opções e vínculos (`ON DELETE CASCADE`).
Os arquivos das imagens são apagados do disco **depois** do commit (ver abaixo).

| Status | Quando |
|---|---|
| 204 | Excluído |
| 401 | Sem token / token inválido |
| 404 | Não existe |

---

## Armazenamento de imagens (`ImageStorageService`)

| Método | Responsabilidade |
|---|---|
| `String save(Integer productId, String mimeType, String content, String field)` | Remove prefixo `data:...;base64,`, remove espaços/quebras de linha, decodifica com o decodificador **estrito** (`Base64.getDecoder()`), valida tamanho e formato, grava em `{UPLOAD_DIR}/products/{productId}/{uuid}.{ext}` e devolve o caminho relativo `products/{productId}/{uuid}.{ext}` |
| `void deleteAfterCommit(String path)` | Apaga o arquivo após o commit; se não existir, apenas loga (não falha) |
| `void deleteProductFolderAfterCommit(Integer productId)` | Apaga a pasta do produto após o commit (usado no DELETE) |
| `String toUrl(String path)` | `app.base-url + "/media/" + path` |

- **Validação do conteúdo:** o formato real é detectado pelos primeiros bytes (assinatura PNG, JPEG,
  GIF, WEBP — enum `ImageFormat`). Conteúdo que não é imagem → 400 `"O conteúdo enviado não é uma
  imagem suportada"`; conteúdo de outro formato que o `type` → 400 em `images[i].type`.
- Extensão pelo formato detectado: `png`, `jpg`, `webp`, `gif`. Nunca usar nome vindo do cliente
  (evita *path traversal*); caminhos que saiam da pasta de uploads são recusados.
- Base64 inválido (`IllegalArgumentException`) → `BadRequestException("Imagem em base64 inválida")`.
- **Consistência com a transação:** arquivos novos gravados durante uma transação que falhar devem ser
  apagados; arquivos de imagens removidas só são apagados se a transação confirmar. Usar
  `TransactionSynchronizationManager.registerSynchronization(...)` com `afterCompletion(status)`.

Servir os arquivos em `/media/**` (`WebConfig`):

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = Path.of(props.uploadDir()).toAbsolutePath().normalize().toUri().toString();
    registry.addResourceHandler("/media/**").addResourceLocations(location); // termina com "/"
}
```

---

## Critérios de aceite

**Busca**
- [ ] Sem params → até 12 produtos com todos os campos, `total` correto.
- [ ] `fields=name,images,price` → itens só com esses três campos.
- [ ] `match=tênis` encontra "Tênis Runner" (nome) e "Ótimo tênis para corrida" (descrição).
- [ ] `category_ids=1,2` → produtos da categoria 1 **ou** 2, sem repetição, e `total` sem duplicados.
- [ ] `price-range=100-200` inclui produtos de 100,00 e 200,00; `price-range=200-100` → 400; `price-range=abc` → 400.
- [ ] `option[45]=GG,PP` → só produtos cuja opção 45 tenha `GG` ou `PP`; o valor `M` não casa com `MM`.
- [ ] `option[45]=GG` funciona com e sem os colchetes codificados.
- [ ] Combinação `match` + `category_ids` + `price-range` aplica todos os filtros.
- [ ] Busca com 50 produtos executa um número **constante** de queries (conferir com `spring.jpa.show-sql=true`), não 1 + 3×N.

**Criação**
- [ ] Payload do enunciado → 201; `GET /v1/product/{id}` devolve imagens com URL acessível no navegador.
- [ ] `radius: "4px"` grava `4`; `value` (singular) é aceito como `values`.
- [ ] Sem `name`, sem `price` ou `price_with_discount > price` → 400.
- [ ] `category_ids` com id inexistente → 400 e **nada** é gravado (nem produto, nem arquivo).
- [ ] Base64 inválido → 400; `type: "application/pdf"` → 400; `shape: "triangle"` → 400.
- [ ] Sem token → 401.

**Atualização**
- [ ] Payload do enunciado → 204; imagem 2 removida (linha e arquivo), imagem 3 com arquivo novo,
      imagem 1 intacta, imagem nova criada; opção 1 removida, opção 2 com `radius 10` e novos valores,
      opção "Tipo" criada.
- [ ] `category_ids` substitui os vínculos; omitido não altera.
- [ ] Imagem/opção de outro produto → 400. Produto inexistente → 404. Sem token → 401.

**Exclusão**
- [ ] 204; `GET` seguinte → 404; arquivos da pasta `products/{id}` removidos; categorias continuam existindo.
