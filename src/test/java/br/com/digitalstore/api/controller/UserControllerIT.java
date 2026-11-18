package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.IntegrationTestBase;
import br.com.digitalstore.api.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends IntegrationTestBase {

    private static final String VALID_PAYLOAD = """
            {
              "firstname": "user firstname",
              "surname": "user surname",
              "email": "user@mail.com",
              "password": "123@123",
              "confirmPassword": "123@123"
            }
            """;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void deveCadastrarUsuarioSemExporSenha() throws Exception {
        mockMvc.perform(post("/v1/user").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/v1/user/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstname").value("user firstname"))
                .andExpect(jsonPath("$.email").value("user@mail.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmail("user@mail.com").orElseThrow();
        assertThat(saved.getPassword()).startsWith("$2").isNotEqualTo("123@123");
    }

    @Test
    void deveRetornar400QuandoSenhasNaoConferem() throws Exception {
        mockMvc.perform(post("/v1/user").contentType(MediaType.APPLICATION_JSON).content(
                        VALID_PAYLOAD.replace("\"confirmPassword\": \"123@123\"", "\"confirmPassword\": \"outra\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("confirmPassword"));
    }

    @Test
    void deveRetornar400ComEmailDuplicadoIgnorandoMaiusculas() throws Exception {
        createUser("user@mail.com", "123@123");

        mockMvc.perform(post("/v1/user").contentType(MediaType.APPLICATION_JSON).content(
                        VALID_PAYLOAD.replace("user@mail.com", "USER@mail.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    @Test
    void deveRetornar400ListandoCampoObrigatorio() throws Exception {
        mockMvc.perform(post("/v1/user").contentType(MediaType.APPLICATION_JSON).content(
                        VALID_PAYLOAD.replace("\"firstname\": \"user firstname\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("firstname")));
    }

    @Test
    void cadastroNaoExigeToken() throws Exception {
        mockMvc.perform(post("/v1/user").header("Authorization", "Bearer invalido")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_PAYLOAD))
                .andExpect(status().isCreated());
    }

    @Test
    void deveObterUsuarioPorId() throws Exception {
        User user = createUser("user@mail.com", "123@123");

        mockMvc.perform(get("/v1/user/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("user@mail.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
        mockMvc.perform(get("/v1/user/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário 999999 não encontrado"));
    }

    @Test
    void deveRetornar400ParaIdNaoNumerico() throws Exception {
        mockMvc.perform(get("/v1/user/abc"))
                .andExpect(status().isBadRequest());
    }

    private static final String UPDATE_PAYLOAD = """
            {"firstname": "Novo", "surname": "Nome", "email": "novo@mail.com"}
            """;

    @Test
    void deveRetornar401AoAtualizarSemToken() throws Exception {
        User user = createUser("user@mail.com", "123@123");

        mockMvc.perform(put("/v1/user/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar404AoAtualizarInexistente() throws Exception {
        mockMvc.perform(put("/v1/user/999999").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_PAYLOAD))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarUsuario() throws Exception {
        User user = createUser("user@mail.com", "123@123");

        mockMvc.perform(put("/v1/user/{id}", user.getId()).header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_PAYLOAD))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/v1/user/{id}", user.getId()))
                .andExpect(jsonPath("$.firstname").value("Novo"))
                .andExpect(jsonPath("$.surname").value("Nome"))
                .andExpect(jsonPath("$.email").value("novo@mail.com"));
    }

    @Test
    void deveRetornar400AoUsarEmailDeOutroUsuario() throws Exception {
        User user = createUser("user@mail.com", "123@123");
        createUser("outro@mail.com", "123@123");

        mockMvc.perform(put("/v1/user/{id}", user.getId()).header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_PAYLOAD.replace("novo@mail.com", "outro@mail.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void devePermitirManterOProprioEmail() throws Exception {
        User user = createUser("user@mail.com", "123@123");

        mockMvc.perform(put("/v1/user/{id}", user.getId()).header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_PAYLOAD.replace("novo@mail.com", "user@mail.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar400AoAtualizarComDadosInvalidos() throws Exception {
        User user = createUser("user@mail.com", "123@123");

        mockMvc.perform(put("/v1/user/{id}", user.getId()).header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstname": "", "surname": "Nome", "email": "nao-e-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("firstname")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")));
    }

    @Test
    void deveRetornar401AoExcluirSemToken() throws Exception {
        User user = createUser("user@mail.com", "123@123");

        mockMvc.perform(delete("/v1/user/{id}", user.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar404AoExcluirInexistente() throws Exception {
        mockMvc.perform(delete("/v1/user/999999").header("Authorization", bearerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveExcluirUsuario() throws Exception {
        User user = createUser("user@mail.com", "123@123");

        mockMvc.perform(delete("/v1/user/{id}", user.getId()).header("Authorization", bearerToken()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/v1/user/{id}", user.getId()))
                .andExpect(status().isNotFound());
    }
}
