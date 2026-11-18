package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.IntegrationTestBase;
import br.com.digitalstore.api.model.Category;
import br.com.digitalstore.api.model.Product;
import br.com.digitalstore.api.model.ProductImage;
import br.com.digitalstore.api.model.ProductOption;
import br.com.digitalstore.api.repository.CategoryRepository;
import br.com.digitalstore.api.repository.ProductRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductSearchIT extends IntegrationTestBase {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Category running;
    private Category casual;
    private Category offers;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        running = category("Running", "running");
        casual = category("Casual", "casual");
        offers = category("Offers", "offers");
    }

    private Category category(String name, String slug) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        return categoryRepository.save(c);
    }

    private Product product(String name, String description, String price, Set<Category> categories,
                            String sizes) {
        Product p = new Product();
        p.setName(name);
        p.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "")
                + "-" + System.nanoTime());
        p.setDescription(description);
        p.setPrice(new BigDecimal(price));
        p.setPriceWithDiscount(new BigDecimal(price));
        p.setCategories(new HashSet<>(categories));
        if (sizes != null) {
            ProductOption option = new ProductOption();
            option.setTitle("Tamanho");
            option.setValues(sizes);
            p.addOption(option);
        }
        ProductImage image = new ProductImage();
        image.setPath("products/x/" + System.nanoTime() + ".png");
        p.addImage(image);
        return productRepository.save(p);
    }

    private Integer optionId(Product p) {
        return p.getOptions().getFirst().getId();
    }

    @Test
    void buscaPadraoRetornaTodosOsCampos() throws Exception {
        product("Tênis Runner", "Leve", "150.00", Set.of(running), "P,M,G");

        mockMvc.perform(get("/v1/product/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.limit").value(12))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Tênis Runner"))
                .andExpect(jsonPath("$.data[0].price_with_discount").value(150.0))
                .andExpect(jsonPath("$.data[0].category_ids", contains(running.getId())))
                .andExpect(jsonPath("$.data[0].images", hasSize(1)))
                .andExpect(jsonPath("$.data[0].options[0].values", contains("P", "M", "G")));
    }

    @Test
    void paginacaoEFields() throws Exception {
        for (int i = 0; i < 15; i++) {
            product("Produto " + i, null, "10.00", Set.of(), null);
        }

        mockMvc.perform(get("/v1/product/search").param("limit", "10").param("page", "2")
                        .param("fields", "name,images,price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.data[0].name").value("Produto 10"))
                .andExpect(jsonPath("$.data[0].images").isArray())
                .andExpect(jsonPath("$.data[0].price").value(10.0))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].options").doesNotExist());

        mockMvc.perform(get("/v1/product/search").param("limit", "-1"))
                .andExpect(jsonPath("$.data", hasSize(15)));
    }

    @Test
    void matchBuscaNoNomeENaDescricaoSemDiferenciarMaiusculas() throws Exception {
        product("Tênis Runner", "Leve", "100.00", Set.of(), null);
        product("Meia esportiva", "Ótima com o seu TÊNIS de corrida", "20.00", Set.of(), null);
        product("Boné", "Aba reta", "50.00", Set.of(), null);

        mockMvc.perform(get("/v1/product/search").param("match", "tênis"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("Tênis Runner", "Meia esportiva")));
    }

    @Test
    void matchEscapaCuringasDoLike() throws Exception {
        product("Desconto 100%", null, "10.00", Set.of(), null);
        product("Desconto 1000", null, "10.00", Set.of(), null);

        mockMvc.perform(get("/v1/product/search").param("match", "100%"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Desconto 100%"));
    }

    @Test
    void categoryIdsRetornaProdutosDeQualquerCategoriaSemDuplicar() throws Exception {
        product("Em duas categorias", null, "10.00", Set.of(running, casual), null);
        product("Só running", null, "10.00", Set.of(running), null);
        product("Só offers", null, "10.00", Set.of(offers), null);

        mockMvc.perform(get("/v1/product/search")
                        .param("category_ids", running.getId() + "," + casual.getId()))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("Em duas categorias", "Só running")));
    }

    @Test
    void priceRangeEInclusivo() throws Exception {
        product("Cem", null, "100.00", Set.of(), null);
        product("Cento e cinquenta", null, "150.00", Set.of(), null);
        product("Duzentos", null, "200.00", Set.of(), null);
        product("Duzentos e um", null, "200.01", Set.of(), null);
        product("Noventa e nove", null, "99.99", Set.of(), null);

        mockMvc.perform(get("/v1/product/search").param("price-range", "100-200"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("Cem", "Cento e cinquenta", "Duzentos")));
    }

    @Test
    void filtroPorOpcaoCasaValorExato() throws Exception {
        Product camiseta = product("Camiseta", null, "50.00", Set.of(), "PP,M,GG");
        Product bermuda = product("Bermuda", null, "60.00", Set.of(), "MM,G");

        Integer opt = optionId(camiseta);
        mockMvc.perform(get("/v1/product/search").param("option[" + opt + "]", "GG,XG"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Camiseta"));

        mockMvc.perform(get("/v1/product/search").param("option[" + optionId(bermuda) + "]", "M"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void filtrosSaoCombinadosComE() throws Exception {
        product("Tênis Runner", null, "150.00", Set.of(running), null);
        product("Tênis Casual", null, "150.00", Set.of(casual), null);
        product("Tênis Runner Pro", null, "500.00", Set.of(running), null);

        mockMvc.perform(get("/v1/product/search")
                        .param("match", "tênis")
                        .param("category_ids", String.valueOf(running.getId()))
                        .param("price-range", "100-200"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Tênis Runner"));
    }

    @Test
    void parametrosInvalidosRetornam400() throws Exception {
        mockMvc.perform(get("/v1/product/search").param("price-range", "200-100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price-range"));
        mockMvc.perform(get("/v1/product/search").param("price-range", "abc")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/product/search").param("category_ids", "1,x")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/product/search").param("option[abc]", "GG")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/product/search").param("option[1]", " , ")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/product/search").param("fields", "name,foo")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/product/search").param("limit", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void buscaExecutaNumeroConstanteDeQueries() throws Exception {
        for (int i = 0; i < 30; i++) {
            product("Produto " + i, null, "10.00", Set.of(running, casual), "P,M");
        }
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        mockMvc.perform(get("/v1/product/search").param("limit", "-1"))
                .andExpect(jsonPath("$.data", hasSize(30)));

        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(6);
    }

    @Test
    void listaVaziaQuandoNadaCasa() throws Exception {
        mockMvc.perform(get("/v1/product/search").param("match", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }
}
