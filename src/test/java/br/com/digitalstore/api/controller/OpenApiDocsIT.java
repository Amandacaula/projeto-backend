package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocsIT extends IntegrationTestBase {

    @Test
    void exposesDocumentationDeclaredInControllerInterfaces() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/v1/user/token'].post.tags[0]").value("Autenticação"))
                .andExpect(jsonPath("$.paths['/v1/category/search'].get.tags[0]").value("Categorias"))
                .andExpect(jsonPath("$.paths['/v1/product/search'].get.tags[0]").value("Produtos"))
                .andExpect(jsonPath("$.paths['/v1/user/{id}'].get.tags[0]").value("Usuários"))
                .andExpect(jsonPath("$.paths['/v1/user/token'].post.summary")
                        .value("Gera um token JWT a partir de email e senha"))
                .andExpect(jsonPath("$.paths['/v1/category/{id}'].delete.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/v1/product/search'].get.parameters[?(@.name == 'price-range')].description")
                        .value(hasItems("Faixa de preço inclusiva min-max")))
                .andExpect(jsonPath("$.paths['/v1/product/{id}'].put.responses['404'].description")
                        .value("Produto não encontrado"));
    }
}
