# 09 — Testes

O enunciado pede Jest; o equivalente em Java é **JUnit 5** (runner/asserções), **Mockito** (mocks),
**MockMvc** (requisições HTTP simuladas) e **Testcontainers** (PostgreSQL real em container).

## Pirâmide

| Nível | Ferramentas | O que cobre | Exemplo |
|---|---|---|---|
| Unitário | JUnit 5 + Mockito | Regras dos services, sem Spring nem banco | `UserServiceTest`: senhas diferentes → `BadRequestException` |
| Unitário | JUnit 5 | Utilitários puros | `SearchParamsTest`, `FieldSelectorTest`, `RadiusParserTest`, `ImageStorageServiceTest` (com `@TempDir`) |
| Integração | `@SpringBootTest` + MockMvc + Testcontainers | Fluxo HTTP completo: segurança, validação, JSON, banco, migrations | `ProductSearchIT`: filtros e paginação |

Prioridade: os **critérios de aceite** das specs 04, 06, 07 e 08 viram testes de integração;
regras de negócio isoladas viram testes unitários.

## Configuração de integração

```java
/** Base dos testes de integração: sobe um PostgreSQL real e aplica as migrations Flyway. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    @ServiceConnection   // configura spring.datasource.* automaticamente
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    /** Cria um usuário e devolve "Bearer <token>" para as rotas protegidas. */
    protected String bearerToken() throws Exception { ... }
}
```

- `src/test/resources/application-test.yml`: `app.jwt.secret` fixo de teste, `app.upload-dir` em pasta temporária.
- Limpar dados entre testes (`@Sql` com `TRUNCATE ... RESTART IDENTITY CASCADE` ou repositórios `deleteAll()`),
  para que a ordem dos testes não importe.
- Testcontainers exige o Docker rodando — o mesmo Docker do ambiente (spec 02).

## Exemplo de teste de integração

```java
class CategoryControllerIT extends IntegrationTestBase {

    @Test
    void deveRetornar401AoCriarCategoriaSemToken() throws Exception {
        mockMvc.perform(post("/v1/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"Shoes","slug":"shoes","use_in_menu":true}
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deveCriarCategoriaComToken() throws Exception {
        mockMvc.perform(post("/v1/category")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"Shoes","slug":"shoes","use_in_menu":true}
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.use_in_menu").value(true));
    }
}
```

## Conjunto mínimo de testes

| Classe | Casos |
|---|---|
| `AuthIT` | token válido (200), senha errada (400), email inexistente (400), rota protegida sem token / token inválido (401) |
| `UserControllerIT` | cadastro 201 sem `password` na resposta, senhas diferentes 400, email duplicado 400, GET 200/404, PUT 204/401/404, DELETE 204/401/404 |
| `CategoryControllerIT` | busca padrão, `limit=-1`, paginação, `fields`, `use_in_menu`, params inválidos 400, CRUD com 201/204/400/401/404 |
| `ProductControllerIT` | criação com imagem base64 e opções (201), validações 400, update com cada operação de imagem/opção (204), delete 204 + arquivo removido |
| `ProductSearchIT` | cada filtro isolado + combinação, `total` sem duplicados com `category_ids` |
| `UserServiceTest`, `ProductServiceTest` | regras de negócio com repositórios mockados |
| `SearchParamsTest`, `FieldSelectorTest`, `RadiusParserTest` | parsing e validação de parâmetros |

## Comandos

```bash
./mvnw test
```

```bash
./mvnw verify
```

Opcional: plugin **JaCoCo** para relatório de cobertura em `target/site/jacoco/index.html`
(meta sugerida: ≥ 70% em `service/`).

## Testes manuais

Além do Swagger UI, versionar uma coleção (Postman ou Insomnia, exportada em `docs/`) com todas as
rotas e uma variável `token` preenchida pela resposta de `POST /v1/user/token`. Facilita a demonstração
para o avaliador.

## Critérios de aceite

- [ ] `./mvnw test` passa em máquina limpa com Docker rodando.
- [ ] Todo endpoint tem ao menos um teste de sucesso e um de erro.
- [ ] Nenhum teste depende de dados criados por outro teste.
