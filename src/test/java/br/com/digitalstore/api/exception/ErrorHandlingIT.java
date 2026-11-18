package br.com.digitalstore.api.exception;

import br.com.digitalstore.api.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorHandlingIT extends IntegrationTestBase {

    @Test
    void jsonMalformadoDeveRetornar400() throws Exception {
        mockMvc.perform(post("/v1/user/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido ou malformado"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void contentTypeDiferenteDeJsonDeveRetornar400() throws Exception {
        mockMvc.perform(post("/v1/user/token")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=user"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Content-Type deve ser application/json"));
    }

    @Test
    void rotaInexistenteDeveRetornar404EmJson() throws Exception {
        mockMvc.perform(get("/v1/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/v1/nao-existe"));
    }
}
