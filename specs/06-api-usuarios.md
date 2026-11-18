# 06 — API de Usuários

Base: `/v1/user` · Controller: `UserController` · Service: `UserService`

O token (`POST /v1/user/token`) está na spec 04.

## Modelos

**`UserResponse`** — nunca contém `password`

```json
{
  "id": 1,
  "firstname": "user firstname",
  "surname": "user surname",
  "email": "user@mail.com"
}
```

---

## `GET /v1/user/{id}` — obter usuário

Público.

| Status | Quando | Corpo |
|---|---|---|
| 200 | Usuário existe | `UserResponse` |
| 400 | `id` não numérico | `ErrorResponse` |
| 404 | Usuário não existe | `ErrorResponse` |

---

## `POST /v1/user` — cadastrar usuário

Público (decisão D2). Header `Content-Type: application/json`.

**Payload**

```json
{
  "firstname": "user firstname",
  "surname": "user surname",
  "email": "user@mail.com",
  "password": "123@123",
  "confirmPassword": "123@123"
}
```

**Validação**

| Campo | Regras |
|---|---|
| `firstname` | obrigatório, máx. 100 |
| `surname` | obrigatório, máx. 100 |
| `email` | obrigatório, formato válido, máx. 150, **único** (comparação sem diferenciar maiúsculas) |
| `password` | obrigatório, mín. 6 |
| `confirmPassword` | obrigatório, igual a `password` (validado no service → erro no campo `confirmPassword`) |

**Comportamento**

1. `trim()` nos textos, email em minúsculo.
2. Se `password != confirmPassword` → 400.
3. Se `existsByEmail` → 400 `"E-mail já cadastrado"`.
4. Salva com `password = passwordEncoder.encode(password)`.

| Status | Quando | Corpo |
|---|---|---|
| 201 | Criado | `UserResponse` + header `Location: /v1/user/{id}` |
| 400 | Validação, senhas diferentes, email duplicado | `ErrorResponse` |

---

## `PUT /v1/user/{id}` — atualizar usuário

**Requer token.** Header `Content-Type: application/json`.

**Payload**

```json
{
  "firstname": "user firstname",
  "surname": "user surname",
  "email": "user@mail.com"
}
```

**Validação:** mesmas regras de `firstname`, `surname` e `email` do cadastro (todos obrigatórios).
A senha **não** é alterada por este endpoint.

**Comportamento**

1. Busca o usuário → 404 se não existir.
2. Se o email mudou e já pertence a **outro** usuário → 400 `"E-mail já cadastrado"`.
3. Atualiza os campos; `updated_at` é renovado pelo `@UpdateTimestamp`.

| Status | Quando |
|---|---|
| 204 | Atualizado (sem corpo) |
| 400 | Validação ou email duplicado |
| 401 | Sem token / token inválido |
| 404 | Usuário não existe |

---

## `DELETE /v1/user/{id}` — excluir usuário

**Requer token.**

| Status | Quando |
|---|---|
| 204 | Excluído (sem corpo) |
| 401 | Sem token / token inválido |
| 404 | Usuário não existe |

---

## Critérios de aceite

- [ ] Cadastro válido → 201; o corpo não tem `password`; no banco a senha começa com `$2a$`/`$2b$` (bcrypt).
- [ ] Cadastro com `confirmPassword` diferente → 400 com erro no campo `confirmPassword`.
- [ ] Cadastro com email já usado (inclusive `USER@mail.com` vs `user@mail.com`) → 400.
- [ ] Cadastro sem `firstname` → 400 listando o campo em `errors`.
- [ ] `GET /v1/user/1` existente → 200; `GET /v1/user/999` → 404.
- [ ] `PUT` sem token → 401; com token e id inexistente → 404; válido → 204 e o `GET` reflete a mudança.
- [ ] `PUT` trocando para o email de outro usuário → 400; mantendo o próprio email → 204.
- [ ] `DELETE` sem token → 401; inexistente → 404; válido → 204 e o `GET` seguinte → 404.
