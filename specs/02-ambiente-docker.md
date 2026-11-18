# 02 — Ambiente e Docker

## Objetivo

Subir banco + API com **um comando** (`docker compose up --build`), sem instalar PostgreSQL
na máquina. Para desenvolvimento diário, também é possível subir só o banco no Docker e rodar
a API pela IDE com DevTools (restart automático, o "Nodemon" do Java).

## Pré-requisitos

- Docker Desktop (com Docker Compose v2)
- JDK 21 e IDE (IntelliJ / VS Code) — apenas para desenvolvimento local
- Git

## Arquivos

### `.env.example` (commitado) → copiar para `.env` (não commitado)

```dotenv
# Banco
DB_NAME=digital_store
DB_USER=digital_store
DB_PASSWORD=digital_store
DB_HOST=localhost
DB_PORT=5432

# API
APP_PORT=8080
APP_BASE_URL=http://localhost:8080
UPLOAD_DIR=./uploads

# JWT — HS256 exige no mínimo 32 caracteres (256 bits)
JWT_SECRET=troque-esta-chave-por-uma-com-32-caracteres-ou-mais
JWT_EXPIRATION_MINUTES=60
```

### `docker-compose.yml`

```yaml
services:
  db:
    image: postgres:17-alpine
    container_name: digital-store-db
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME}"]
      interval: 5s
      timeout: 5s
      retries: 10

  api:
    build: .
    container_name: digital-store-api
    depends_on:
      db:
        condition: service_healthy   # só sobe quando o Postgres aceitar conexões
    environment:
      DB_HOST: db                    # nome do serviço na rede do compose
      DB_PORT: 5432                  # porta interna (não a do host)
      DB_NAME: ${DB_NAME}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MINUTES: ${JWT_EXPIRATION_MINUTES}
      APP_BASE_URL: ${APP_BASE_URL}
      UPLOAD_DIR: /app/uploads
    ports:
      - "${APP_PORT:-8080}:8080"
    volumes:
      - uploads:/app/uploads

volumes:
  pgdata:
  uploads:
```

> Use `postgres:17`. A imagem `postgres:18` mudou o caminho do volume de dados
> (`/var/lib/postgresql`), e o mapeamento acima deixaria de funcionar.

Opcional: adicionar um serviço `pgadmin` (`dpage/pgadmin4`) na porta 5050 para inspecionar o banco.

### `Dockerfile` (multi-stage)

```dockerfile
# 1) Build: compila o jar com Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline          # cache das dependências
COPY src ./src
RUN mvn -B -q package -DskipTests

# 2) Runtime: só a JRE + o jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `.dockerignore`

```
target/
.git/
.idea/
.vscode/
.env
uploads/
```

### `.gitignore` (acrescentar ao gerado pelo Initializr)

```
.env
uploads/
```

### `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: digital-store-api
  # Lê o .env da raiz quando a API roda fora do Docker (equivalente ao dotenv)
  config:
    import: optional:file:.env[.properties]
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:digital_store}
    username: ${DB_USER:digital_store}
    password: ${DB_PASSWORD:digital_store}
  jpa:
    hibernate:
      ddl-auto: validate          # o schema é do Flyway; o Hibernate só confere
    open-in-view: false
    properties:
      hibernate:
        default_batch_fetch_size: 50   # evita N+1 em imagens/opções/categorias
  flyway:
    enabled: true
    locations: classpath:db/migration
  jackson:
    property-naming-strategy: SNAKE_CASE

server:
  port: 8080
  tomcat:
    # Permite ?option[45]=GG sem precisar codificar os colchetes (%5B %5D)
    relaxed-query-chars: "[,]"

app:
  base-url: ${APP_BASE_URL:http://localhost:8080}
  upload-dir: ${UPLOAD_DIR:./uploads}
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:60}

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

Ler o bloco `app.*` com uma classe `@ConfigurationProperties(prefix = "app")`.

## Fluxos de uso

**A) Tudo no Docker (avaliador / demonstração)**

```bash
cp .env.example .env
docker compose up --build
```

API em `http://localhost:8080`, Swagger em `http://localhost:8080/swagger-ui.html`.

**B) Desenvolvimento (banco no Docker, API na IDE com hot reload)**

```bash
docker compose up -d db
./mvnw spring-boot:run
```

Com `DB_HOST=localhost` no `.env`. O DevTools reinicia a aplicação a cada compilação.

**Resetar o banco** (apaga os dados e reaplica as migrations):

```bash
docker compose down -v
```

## Critérios de aceite

- [ ] `docker compose up --build` em uma máquina limpa sobe banco e API sem passos manuais além de copiar o `.env`.
- [ ] A API só inicia depois do healthcheck do banco (sem erro de conexão no log).
- [ ] As migrations Flyway rodam no startup e o log mostra `Successfully applied 6 migrations`.
- [ ] Imagens enviadas continuam disponíveis após `docker compose restart` (volume `uploads`).
- [ ] Nenhum segredo commitado: `.env` está no `.gitignore`, só `.env.example` vai para o Git.
