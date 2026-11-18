# Specs — Projeto Back-end (Java / Spring Boot)

Este diretório contém as especificações para implementar o projeto descrito em
[`docs/ENUNCIADO.md`](../docs/ENUNCIADO.md) (enunciado oficial, escrito para Node.js),
adaptado para **Java 21 + Spring Boot + PostgreSQL + Docker**, com **MVC simplificado**.

> **Prazo de entrega:** 20/11/2026 — envio do link do repositório no GitHub (equipes de até 3 pessoas).

## Índice

| # | Spec | Conteúdo |
|---|------|----------|
| 00 | [Visão geral e decisões](00-visao-geral.md) | Stack, equivalência Node → Java, ambiguidades do enunciado e decisões tomadas |
| 01 | [Arquitetura](01-arquitetura.md) | MVC simplificado, estrutura de pacotes, convenções, dependências do `pom.xml` |
| 02 | [Ambiente e Docker](02-ambiente-docker.md) | Dockerfile, docker-compose, `.env`, `application.yml`, fluxo de desenvolvimento |
| 03 | [Banco de dados](03-banco-de-dados.md) | Modelo ER, migrations Flyway (SQL completo), mapeamento JPA, consultas eficientes |
| 04 | [Autenticação JWT](04-autenticacao-jwt.md) | Spring Security, geração e validação do token, rotas públicas/protegidas |
| 05 | [Erros e validação](05-erros-e-validacao.md) | Formato padrão de erro, mapeamento exceção → status HTTP |
| 06 | [API — Usuários](06-api-usuarios.md) | `GET/POST/PUT/DELETE /v1/user` |
| 07 | [API — Categorias](07-api-categorias.md) | `/v1/category` + busca paginada |
| 08 | [API — Produtos](08-api-produtos.md) | `/v1/product` + busca com filtros, imagens (base64) e opções |
| 09 | [Testes](09-testes.md) | Estratégia de testes (JUnit 5, Mockito, MockMvc, Testcontainers) |
| 10 | [Documentação](10-documentacao.md) | Swagger (springdoc) e modelo do README de setup |
| 11 | [Roadmap](11-roadmap.md) | Ordem de implementação, plano por dia, divisão da equipe, checklist de avaliação |

## Como usar estas specs

1. Leia **00** e **01** antes de escrever código — elas fixam as decisões que o resto assume.
2. Implemente na ordem do **roadmap (11)**: infraestrutura → banco → erros → auth → usuários → categorias → produtos → docs.
3. Cada spec de endpoint (06–08) tem uma seção **Critérios de aceite**: use-os como lista de casos de teste.
4. Quando uma decisão mudar, atualize a spec no mesmo commit do código.
