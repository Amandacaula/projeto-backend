# Digital Store API — Projeto Back-end

API REST de e-commerce com CRUD de **usuários**, **categorias** e **produtos** (com imagens e opções),
autenticação **JWT**, documentação **Swagger** e ambiente **Docker**.

## Tecnologias

O enunciado sugere ferramentas do ecossistema Node; esta implementação usa os equivalentes em Java:

| Enunciado (Node) | Usado aqui | Papel |
|---|---|---|
| Node.js | Java 21 | Runtime |
| Express.js | Spring Boot 4.1 (Spring Web MVC) | Rotas da API |
| Dotenv | Arquivo `.env` (Docker Compose + `spring.config.import`) | Configuração |
| Nodemon | Spring Boot DevTools | Restart automático |
| MySQL | PostgreSQL 17 | Banco de dados |
| Sequelize | Spring Data JPA (Hibernate 7) + Flyway | ORM + migrations |
| bcrypt | `BCryptPasswordEncoder` | Hash de senha |
| JWT | Spring Security 7 + jjwt (HS256) | Autenticação |
| Jest | JUnit 5 + Mockito + MockMvc + Testcontainers | Testes |
| — | springdoc-openapi (Swagger UI) | Documentação |
| — | Docker + Docker Compose | Ambiente |

## Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (com Docker Compose v2)
- Para desenvolver ou rodar os testes: JDK 21 ou mais recente (o Maven Wrapper `mvnw` já vem no projeto)

## Como rodar

```bash
git clone <url-do-repositorio>
cd projeto-backend
cp .env.example .env
docker compose up --build
```

| O quê | Onde |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| PostgreSQL | `localhost:5432` (usuário/senha do `.env`) |

As migrations do Flyway rodam automaticamente na subida da API.

## Como usar a API

1. **Crie um usuário** (rota pública):

   ```bash
   curl -X POST http://localhost:8080/v1/user -H "Content-Type: application/json" -d '{"firstname":"Ana","surname":"Silva","email":"ana@mail.com","password":"123@123","confirmPassword":"123@123"}'
   ```

2. **Gere o token**:

   ```bash
   curl -X POST http://localhost:8080/v1/user/token -H "Content-Type: application/json" -d '{"email":"ana@mail.com","password":"123@123"}'
   ```

3. **Use o token** nas rotas `POST`, `PUT` e `DELETE` (header `Authorization: Bearer <token>`).
   No Swagger, clique em **Authorize** e cole o token.

   ```bash
   curl -X POST http://localhost:8080/v1/category -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{"name":"Shoes","slug":"shoes","use_in_menu":true}'
   ```

4. **Busque produtos** com filtros (rota pública). No curl, use `-g` para que os colchetes de `option[...]` não sejam interpretados:

   ```bash
   curl -g "http://localhost:8080/v1/product/search?match=tenis&category_ids=1,2&price-range=100-200&option[1]=GG,PP&fields=name,price,images"
   ```

### Endpoints

| Método | Rota | Token | Sucesso |
|---|---|---|---|
| `POST` | `/v1/user/token` | — | 200 |
| `GET` | `/v1/user/{id}` | — | 200 |
| `POST` | `/v1/user` | — | 201 |
| `PUT` | `/v1/user/{id}` | ✔ | 204 |
| `DELETE` | `/v1/user/{id}` | ✔ | 204 |
| `GET` | `/v1/category/search` | — | 200 |
| `GET` | `/v1/category/{id}` | — | 200 |
| `POST` | `/v1/category` | ✔ | 201 |
| `PUT` | `/v1/category/{id}` | ✔ | 204 |
| `DELETE` | `/v1/category/{id}` | ✔ | 204 |
| `GET` | `/v1/product/search` | — | 200 |
| `GET` | `/v1/product/{id}` | — | 200 |
| `POST` | `/v1/product` | ✔ | 201 |
| `PUT` | `/v1/product/{id}` | ✔ | 204 |
| `DELETE` | `/v1/product/{id}` | ✔ | 204 |

Erros seguem sempre o mesmo formato:

```json
{
  "timestamp": "2026-11-12T19:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Dados da requisição inválidos",
  "path": "/v1/user",
  "errors": [{ "field": "email", "message": "E-mail inválido" }]
}
```

As imagens de produto são enviadas em base64 e servidas em `GET /media/products/{id}/{arquivo}`.

## Variáveis de ambiente

| Variável | Padrão no `.env.example` | Descrição |
|---|---|---|
| `DB_NAME` | `digital_store` | Nome do banco |
| `DB_USER` | `digital_store` | Usuário do banco |
| `DB_PASSWORD` | `digital_store` | Senha do banco |
| `DB_HOST` | `localhost` | Host do banco quando a API roda fora do Docker |
| `DB_PORT` | `5432` | Porta do banco exposta no host |
| `APP_PORT` | `8080` | Porta da API exposta no host |
| `APP_BASE_URL` | `http://localhost:8080` | URL pública usada nos links das imagens |
| `UPLOAD_DIR` | `./uploads` | Pasta das imagens quando a API roda fora do Docker |
| `JWT_SECRET` | — | Chave do HS256, **mínimo 32 caracteres** (a API não sobe sem ela) |
| `JWT_EXPIRATION_MINUTES` | `60` | Validade do token |

## Desenvolvimento

Banco no Docker e API local com restart automático (DevTools):

```bash
docker compose up -d db
```

```bash
./mvnw spring-boot:run
```

Rodar os testes (precisa do Docker ligado — o Testcontainers sobe um PostgreSQL temporário):

```bash
./mvnw test
```

Apagar os dados e reaplicar as migrations:

```bash
docker compose down -v
```

## Estrutura do projeto

O projeto segue um **MVC simplificado**: `controller` (HTTP) → `service` (regras de negócio) →
`repository` (dados). Entidades JPA ficam em `model` e nunca saem da API; as respostas usam DTOs (`dto`).

```
src/main/java/br/com/digitalstore/api/
├── DigitalStoreApiApplication.java   # ponto de entrada
├── config/        # segurança, Swagger, /media, propriedades
├── controller/    # endpoints REST
├── dto/           # payloads de entrada e saída (records)
├── exception/     # exceções e GlobalExceptionHandler
├── model/         # entidades JPA e enums
├── repository/    # Spring Data JPA + specifications da busca
├── security/      # JWT: geração, filtro e resposta 401
├── service/       # regras de negócio e armazenamento de imagens
└── util/          # paginação, seleção de campos, parsing
src/main/resources/
├── application.yml
└── db/migration/  # migrations Flyway V1..V6
```

## Banco de dados

Seis migrations Flyway criam as tabelas `users`, `categories`, `products`, `product_images`,
`product_options` e `product_categories`, com chaves estrangeiras `ON DELETE CASCADE`, `UNIQUE` em
email e slugs, `CHECK` nos campos enumerados e índices para os filtros da busca. O diagrama ER e o
SQL completo estão em [`specs/03-banco-de-dados.md`](specs/03-banco-de-dados.md).

A busca de produtos evita N+1 (coleções carregadas em lote) e usa `EXISTS` nos filtros por
categoria e opção, para não duplicar produtos nem distorcer o `total` — há um teste que garante
um número constante de queries.

## Decisões de projeto

O enunciado tem algumas inconsistências; estas foram as escolhas (lista completa em
[`specs/00-visao-geral.md`](specs/00-visao-geral.md)):

- **401** quando o token falta ou é inválido (o enunciado cita 400 em um ponto e 401 em todos os endpoints).
- `POST /v1/user` (cadastro) e `POST /v1/user/token` (login) são **públicos**; sem isso ninguém obteria o primeiro token.
- Login com credenciais erradas retorna **400** com mensagem genérica.
- Preços em `NUMERIC(10,2)` / `BigDecimal` em vez de FLOAT, para não haver erro de arredondamento.
- `shape` e `type` das opções usam `VARCHAR` + `CHECK` (mesma restrição de um ENUM).
- `values` é palavra reservada em SQL: a coluna é criada entre aspas.
- Nas opções, `value` e `values` são aceitos; `radius` aceita `4`, `"4"` ou `"4px"`.
- Email e slugs são únicos; duplicados retornam 400.
- O conteúdo das imagens é conferido (assinatura do arquivo), não só o `type` declarado.

## Testes

101 testes automatizados: integração ponta a ponta com MockMvc + PostgreSQL real (Testcontainers)
cobrindo os critérios de aceite de cada endpoint, e testes unitários das regras de negócio e
utilitários.
