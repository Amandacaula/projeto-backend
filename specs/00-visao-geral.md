# 00 — Visão geral e decisões

## Objetivo

API REST de uma loja virtual com CRUD de **usuários**, **categorias** e **produtos**
(incluindo imagens e opções de produto), protegida por **JWT** nas operações de escrita,
documentada com **Swagger** e executável com **Docker Compose**.

## Stack: equivalência Node → Java

O enunciado lista tecnologias do ecossistema Node. A tabela abaixo mostra o equivalente
adotado e serve como justificativa na apresentação do projeto.

| Enunciado (Node) | Escolha (Java) | Papel |
|---|---|---|
| Node.js | **Java 21 (LTS)** | Runtime |
| Express.js | **Spring Boot 4.1.x** (Spring Web MVC) | Rotas da API |
| Dotenv | Variáveis de ambiente + arquivo `.env` (lido pelo Docker Compose e por `spring.config.import`) | Configuração |
| Nodemon | **Spring Boot DevTools** | Restart automático em desenvolvimento |
| MySQL | **PostgreSQL 17** | Persistência |
| Sequelize | **Spring Data JPA (Hibernate)** + **Flyway** | ORM + migrations |
| bcrypt | `BCryptPasswordEncoder` (Spring Security) | Hash de senha |
| JWT (jsonwebtoken) | **Spring Security** + **jjwt 0.12.x** | Autenticação |
| Jest | **JUnit 5 + Mockito + MockMvc + Testcontainers** | Testes |
| — | **springdoc-openapi 2.x** | Swagger UI |
| — | **Docker + Docker Compose** | Ambiente |
| npm | **Maven** (com Maven Wrapper `mvnw`) | Build e dependências |

> **Versões efetivas:** projeto `digital-store-api` (pacote `br.com.digitalstore.api`) gerado no
> Spring Initializr com **Spring Boot 4.1.2** e Java 21. Na linha 4.x: Spring Security 7, Hibernate 7,
> **Jackson 3** (classes de databind no pacote `tools.jackson.databind`; as anotações continuam em
> `com.fasterxml.jackson.annotation`), `springdoc-openapi` **3.2.0** e `jjwt` **0.13.0**.
> Os starters são modulares (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, ...).

## Escopo funcional

| Recurso | Endpoints |
|---|---|
| Usuários | `GET /v1/user/{id}`, `POST /v1/user`, `PUT /v1/user/{id}`, `DELETE /v1/user/{id}` |
| Token | `POST /v1/user/token` |
| Categorias | `GET /v1/category/search`, `GET /v1/category/{id}`, `POST /v1/category`, `PUT /v1/category/{id}`, `DELETE /v1/category/{id}` |
| Produtos | `GET /v1/product/search`, `GET /v1/product/{id}`, `POST /v1/product`, `PUT /v1/product/{id}`, `DELETE /v1/product/{id}` |

Fora do escopo: listagem de usuários (o enunciado pula o "Requisito 03" da seção de usuários),
papéis/permissões (admin vs cliente), refresh token, front-end.

## Ambiguidades do enunciado e decisões

O enunciado tem inconsistências. Cada decisão abaixo deve ser citada no README final
(seção "Decisões de projeto") — isso mostra ao avaliador que foi uma escolha consciente.

| # | Ponto | O que o enunciado diz | Decisão |
|---|---|---|---|
| D1 | Token ausente/inválido | Seção 05 / Req. 02 diz **400**; todos os endpoints dizem **401** | **401 Unauthorized** — coerente com os endpoints e com a semântica HTTP |
| D2 | Rotas públicas | "Todos os POST, PUT e DELETE devem conter o cabeçalho Authorization" | **Exceções:** `POST /v1/user` (cadastro) e `POST /v1/user/token` (login) são públicos; sem isso ninguém consegue obter o primeiro token. Todos os `GET` são públicos |
| D3 | Login com credenciais erradas | Só lista 200 e 400 | **400 Bad Request** com mensagem genérica "E-mail ou senha inválidos" (não revela qual dos dois errou) |
| D4 | Tipo de `price` / `price_with_discount` | FLOAT | **`NUMERIC(10,2)`** no banco e `BigDecimal` no Java (FLOAT gera erro de arredondamento com dinheiro). No JSON continua sendo número: `119.90` |
| D5 | Colunas ENUM (`shape`, `type`) | ENUM | `VARCHAR(10)` + `CHECK (... IN (...))` + `AttributeConverter` no JPA. Restringe os valores igual a um ENUM e evita mapeamento especial do Hibernate. (Alternativa: `CREATE TYPE ... AS ENUM` — ver spec 03) |
| D6 | Coluna `values` | nome `values` | `values` é **palavra reservada em SQL**: a coluna é criada entre aspas (`"values"`) e mapeada com `@Column(name = "\"values\"")` |
| D7 | Tabela de usuários | "tabela de usuários" | Nome **`users`** (`user` é reservado no PostgreSQL) |
| D8 | `value` vs `values` no payload de opções | Os exemplos usam os dois | Aceitar **ambos** (`@JsonAlias("value")`); a resposta sempre usa `values` |
| D9 | `radius` | Coluna INTEGER, mas payload envia `"4px"` | Aceitar número (`4`) ou string (`"4"`, `"4px"`); gravar o inteiro `4`. Resposta devolve número |
| D10 | Unicidade | Não especificado | `users.email`, `categories.slug` e `products.slug` são **UNIQUE**. Duplicado → **400** |
| D11 | Corpo do 201 | Não especificado | Retornar o recurso criado (mesmo formato do GET por id) + header `Location` |
| D12 | `enabled` das imagens | Padrão 0 no banco | Imagens enviadas pela API são gravadas com `enabled = true`; o DEFAULT do banco continua `false`, como pede o enunciado |
| D13 | `fields` com campo inexistente | Não especificado | **400** com a lista de campos permitidos |
| D14 | `option[45]=GG,PP` | Não especifica o que é 45 | `45` é o **id da opção** (`product_options.id`); retorna produtos cuja opção 45 contém **algum** dos valores |
| D15 | `price-range` | Não diz qual preço | Filtra por `price` (preço cheio), intervalo **inclusivo** |
| D16 | Ordem das validações | — | Autenticação primeiro: `PUT /v1/user/999` sem token → **401** (não 404) |
| D17 | Quem pode alterar/excluir | Não especificado | Qualquer usuário autenticado pode alterar qualquer recurso (sem papéis). Melhoria opcional: usuário só altera/exclui a si mesmo (403) |
| D18 | Vírgula em valores de opção | `values` é string separada por vírgula | Valores contendo vírgula são rejeitados com **400** |
| D19 | Respostas JSON | snake_case (`price_with_discount`, `use_in_menu`) | Jackson global em **SNAKE_CASE**; única exceção é `confirmPassword` (camelCase no enunciado) |
