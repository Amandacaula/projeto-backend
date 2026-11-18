package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.IntegrationTestBase;
import br.com.digitalstore.api.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

class AuthIT extends IntegrationTestBase {

    private static final String EMAIL = "user@mail.com";
    private static final String PASSWORD = "123@123";

    @Autowired
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createUser(EMAIL, PASSWORD);
    }

    @Test
    void deveGerarTokenComCredenciaisCorretas() throws Exception {
        mockMvc.perform(post("/v1/user/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "USER@mail.com", "password": "123@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())));
    }

    @Test
    void deveRetornar400ComSenhaErrada() throws Exception {
        mockMvc.perform(post("/v1/user/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@mail.com", "password": "errada"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
    }

    @Test
    void deveRetornar400ComEmailInexistenteEMesmaMensagem() throws Exception {
        mockMvc.perform(post("/v1/user/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "naoexiste@mail.com", "password": "123@123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
    }

    @Test
    void deveRetornar400ListandoCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/v1/user/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password")));
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/v1/category").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Token de autenticação ausente ou inválido"))
                .andExpect(jsonPath("$.path").value("/v1/category"));
    }

    @Test
    void deveRetornar401ComTokenInvalido() throws Exception {
        mockMvc.perform(post("/v1/category")
                        .header("Authorization", "Bearer abc")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar401ComTokenExpirado() throws Exception {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        String expired = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(post("/v1/category")
                        .header("Authorization", "Bearer " + expired)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devePassarPelaSegurancaComTokenValido() throws Exception {
        int status = mockMvc.perform(post("/v1/category")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(401);
    }

    @Test
    void swaggerDeveSerPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
