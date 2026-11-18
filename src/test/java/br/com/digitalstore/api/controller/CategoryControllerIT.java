package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.IntegrationTestBase;
import br.com.digitalstore.api.model.Category;
import br.com.digitalstore.api.model.Product;
import br.com.digitalstore.api.repository.CategoryRepository;
import br.com.digitalstore.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerIT extends IntegrationTestBase {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    private Category category(String name, String slug, boolean useInMenu) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setUseInMenu(useInMenu);
        return categoryRepository.save(c);
    }

    private void createCategories(int count) {
        for (int i = 1; i <= count; i++) {
            category("Categoria " + i, "categoria-" + i, i % 3 == 0);
        }
    }

    @Test
    void buscaPadraoRetorna12ItensDaPrimeiraPagina() throws Exception {
        createCategories(15);

        mockMvc.perform(get("/v1/category/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].use_in_menu").value(false))
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.limit").value(12))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void paginaComLimitEPage() throws Exception {
        createCategories(15);

        mockMvc.perform(get("/v1/category/search").param("limit", "10").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.total").value(15));
    }

    @Test
    void limitMenosUmRetornaTodosEIgnoraPage() throws Exception {
        createCategories(15);

        mockMvc.perform(get("/v1/category/search").param("limit", "-1").param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(15)))
                .andExpect(jsonPath("$.limit").value(-1))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void paginaAlemDoFimRetornaListaVazia() throws Exception {
        createCategories(3);

        mockMvc.perform(get("/v1/category/search").param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void fieldsLimitaOsCamposRetornados() throws Exception {
        createCategories(2);

        mockMvc.perform(get("/v1/category/search").param("fields", "name,slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Categoria 1"))
                .andExpect(jsonPath("$.data[0].slug").value("categoria-1"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].use_in_menu").doesNotExist());
    }

    @Test
    void filtroUseInMenuConsideraOTotal() throws Exception {
        createCategories(9);

        mockMvc.perform(get("/v1/category/search").param("use_in_menu", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].use_in_menu", everyItem(org.hamcrest.Matchers.is(true))))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void parametrosInvalidosRetornam400() throws Exception {
        mockMvc.perform(get("/v1/category/search").param("fields", "foo")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("fields"));
        mockMvc.perform(get("/v1/category/search").param("limit", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/category/search").param("limit", "-2")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/category/search").param("page", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/category/search").param("limit", "abc")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/category/search").param("use_in_menu", "talvez")).andExpect(status().isBadRequest());
    }

    @Test
    void obtemCategoriaPorId() throws Exception {
        Category c = category("Shoes", "shoes", true);

        mockMvc.perform(get("/v1/category/{id}", c.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shoes"))
                .andExpect(jsonPath("$.slug").value("shoes"))
                .andExpect(jsonPath("$.use_in_menu").value(true));

        mockMvc.perform(get("/v1/category/999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/category/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void criaCategoriaComToken() throws Exception {
        mockMvc.perform(post("/v1/category").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Shoes", "slug": "shoes", "use_in_menu": true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/v1/category/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.use_in_menu").value(true));
    }

    @Test
    void useInMenuOmitidoGravaFalse() throws Exception {
        mockMvc.perform(post("/v1/category").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Offers", "slug": "offers"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.use_in_menu").value(false));
    }

    @Test
    void criacaoRejeitaSemTokenSlugDuplicadoESlugInvalido() throws Exception {
        String payload = """
                {"name": "Shoes", "slug": "shoes"}
                """;
        mockMvc.perform(post("/v1/category").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());

        category("Shoes", "shoes", false);
        mockMvc.perform(post("/v1/category").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slug já cadastrado"));

        mockMvc.perform(post("/v1/category").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Tênis", "slug": "Tênis Novo"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("slug"));
    }

    @Test
    void atualizaCategoria() throws Exception {
        Category c = category("Shoes", "shoes", false);

        mockMvc.perform(put("/v1/category/{id}", c.getId()).header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sapatos", "slug": "sapatos", "use_in_menu": true}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/v1/category/{id}", c.getId()))
                .andExpect(jsonPath("$.name").value("Sapatos"))
                .andExpect(jsonPath("$.slug").value("sapatos"))
                .andExpect(jsonPath("$.use_in_menu").value(true));
    }

    @Test
    void atualizacaoRejeitaSlugDeOutraCategoriaEIdInexistente() throws Exception {
        Category c = category("Shoes", "shoes", false);
        category("Offers", "offers", false);

        mockMvc.perform(put("/v1/category/{id}", c.getId()).header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Shoes", "slug": "offers"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/v1/category/999").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "slug": "x"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/v1/category/{id}", c.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "slug": "x"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void excluiCategoriaMantendoOProdutoVinculado() throws Exception {
        Category shoes = category("Shoes", "shoes", false);
        Category offers = category("Offers", "offers", false);
        Product product = new Product();
        product.setName("Tênis");
        product.setSlug("tenis");
        product.setPrice(new BigDecimal("100.00"));
        product.setPriceWithDiscount(new BigDecimal("90.00"));
        product.setCategories(new HashSet<>(Set.of(shoes, offers)));
        product = productRepository.save(product);

        mockMvc.perform(delete("/v1/category/{id}", shoes.getId()).header("Authorization", bearerToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/category/{id}", shoes.getId())).andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/product/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category_ids", hasSize(1)))
                .andExpect(jsonPath("$.category_ids[0]").value(offers.getId()));
    }

    @Test
    void exclusaoSemTokenOuInexistente() throws Exception {
        Category c = category("Shoes", "shoes", false);

        mockMvc.perform(delete("/v1/category/{id}", c.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/category/999").header("Authorization", bearerToken()))
                .andExpect(status().isNotFound());
    }
}
