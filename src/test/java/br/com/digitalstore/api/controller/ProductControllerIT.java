package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.IntegrationTestBase;
import br.com.digitalstore.api.config.AppProperties;
import br.com.digitalstore.api.model.Category;
import br.com.digitalstore.api.repository.CategoryRepository;
import br.com.digitalstore.api.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerIT extends IntegrationTestBase {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AppProperties appProperties;

    private Path uploadRoot;
    private Integer shoesId;
    private Integer offersId;

    @BeforeEach
    void setUp() throws IOException {
        cleanDatabase();
        uploadRoot = Path.of(appProperties.uploadDir()).toAbsolutePath().normalize();
        FileSystemUtils.deleteRecursively(uploadRoot.resolve("products"));
        shoesId = category("Shoes", "shoes").getId();
        offersId = category("Offers", "offers").getId();
    }

    private Category category(String name, String slug) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        return categoryRepository.save(c);
    }

    private String fullPayload() {
        return """
                {
                  "enabled": true,
                  "name": "Produto 01",
                  "slug": "produto-01",
                  "stock": 10,
                  "description": "Descrição do produto 01",
                  "price": 119.90,
                  "price_with_discount": 99.90,
                  "category_ids": [%d, %d],
                  "images": [
                    {"type": "image/png", "content": "%s"},
                    {"type": "image/jpg", "content": "%s"}
                  ],
                  "options": [
                    {"title": "Tamanho", "shape": "square", "radius": "4px", "type": "text", "values": ["PP", "GG", "M"]},
                    {"title": "Cor", "shape": "circle", "type": "color", "value": ["#000", "#333"]}
                  ]
                }
                """.formatted(shoesId, offersId, PNG_BASE64, JPEG_BASE64);
    }

    private String createProduct(String payload) throws Exception {
        return mockMvc.perform(post("/v1/product").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String getProduct(int id) throws Exception {
        return mockMvc.perform(get("/v1/product/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String mediaPath(String url) {
        return url.substring(url.indexOf("/media/"));
    }

    private Path fileOf(String url) {
        return uploadRoot.resolve(mediaPath(url).substring("/media/".length()));
    }

    private long filesUnderProducts() throws IOException {
        Path products = uploadRoot.resolve("products");
        if (!Files.exists(products)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(products)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    @Test
    void criaProdutoComImagensOpcoesECategorias() throws Exception {
        mockMvc.perform(post("/v1/product").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(fullPayload()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/v1/product/\\d+")))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.use_in_menu").value(false))
                .andExpect(jsonPath("$.price").value(119.90))
                .andExpect(jsonPath("$.price_with_discount").value(99.90))
                .andExpect(jsonPath("$.category_ids", contains(shoesId, offersId)))
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.images[0].content", startsWith("http://localhost:8080/media/products/")))
                .andExpect(jsonPath("$.images[1].content", matchesPattern(".*\\.jpg$")))
                .andExpect(jsonPath("$.options[0].radius").value(4))
                .andExpect(jsonPath("$.options[0].shape").value("square"))
                .andExpect(jsonPath("$.options[0].values", contains("PP", "GG", "M")))
                .andExpect(jsonPath("$.options[1].type").value("color"))
                .andExpect(jsonPath("$.options[1].radius").value(0))
                .andExpect(jsonPath("$.options[1].values", contains("#000", "#333")));
    }

    @Test
    void imagemCriadaFicaAcessivelEmMedia() throws Exception {
        String body = createProduct(fullPayload());
        String url = JsonPath.read(body, "$.images[0].content");

        mockMvc.perform(get(mediaPath(url)))
                .andExpect(status().isOk());
        assertThat(Files.exists(fileOf(url))).isTrue();
    }

    @Test
    void camposOpcionaisRecebemValoresPadrao() throws Exception {
        mockMvc.perform(post("/v1/product").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Simples", "slug": "simples", "price": 10, "price_with_discount": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.category_ids", hasSize(0)))
                .andExpect(jsonPath("$.images", hasSize(0)))
                .andExpect(jsonPath("$.options", hasSize(0)));
    }

    @Test
    void criacaoExigeToken() throws Exception {
        mockMvc.perform(post("/v1/product").contentType(MediaType.APPLICATION_JSON).content(fullPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criacaoValidaCamposObrigatoriosEPrecos() throws Exception {
        String token = bearerToken();
        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug": "x", "price": 10, "price_with_discount": 5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "slug": "x", "price_with_discount": 5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"));

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "slug": "x", "price": 10, "price_with_discount": 20}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price_with_discount"));
    }

    @Test
    void slugDuplicadoRetorna400() throws Exception {
        createProduct(fullPayload());

        mockMvc.perform(post("/v1/product").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(fullPayload()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slug já cadastrado"));
    }

    @Test
    void categoriaInexistenteRetorna400ENadaEGravado() throws Exception {
        mockMvc.perform(post("/v1/product").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullPayload().replace("\"category_ids\": [" + shoesId, "\"category_ids\": [99")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Categorias não encontradas: [99]"));

        assertThat(productRepository.count()).isZero();
        assertThat(filesUnderProducts()).isZero();
    }

    @Test
    void falhaDepoisDeGravarImagemDesfazBancoEArquivos() throws Exception {
        mockMvc.perform(post("/v1/product").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "slug": "x", "price": 10, "price_with_discount": 5,
                                 "images": [{"type": "image/png", "content": "%s"}],
                                 "options": [{"title": "Tamanho"}]}
                                """.formatted(PNG_BASE64)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("options[0].values"));

        assertThat(productRepository.count()).isZero();
        assertThat(filesUnderProducts()).isZero();
    }

    @Test
    void imagensInvalidasRetornam400() throws Exception {
        String token = bearerToken();
        String template = """
                {"name": "X", "slug": "x", "price": 10, "price_with_discount": 5,
                 "images": [{"type": "%s", "content": "%s"}]}
                """;

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted("image/png", "@@@ não é base64 @@@")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Imagem em base64 inválida"));

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted("application/pdf", PNG_BASE64)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("images[0].type"));

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted("image/png", "aGVsbG8gd29ybGQ=")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O conteúdo enviado não é uma imagem suportada"));

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted("image/jpeg", PNG_BASE64)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("images[0].type"));
    }

    @Test
    void opcoesInvalidasRetornam400() throws Exception {
        String token = bearerToken();
        String template = """
                {"name": "X", "slug": "x", "price": 10, "price_with_discount": 5, "options": [%s]}
                """;

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted("{\"title\": \"T\", \"shape\": \"triangle\", \"values\": [\"A\"]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para shape: triangle"));

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted("{\"title\": \"T\", \"radius\": \"abc\", \"values\": [\"A\"]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("options[0].radius"));

        mockMvc.perform(post("/v1/product").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted("{\"title\": \"T\", \"values\": [\"A,B\"]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("options[0].values"));
    }

    @Test
    void obtemProdutoOu404() throws Exception {
        createProduct(fullPayload());

        mockMvc.perform(get("/v1/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Produto 01"));
        mockMvc.perform(get("/v1/product/999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/product/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void atualizaAplicandoTodasAsOperacoesDoEnunciado() throws Exception {
        String created = createProduct("""
                {"name": "Produto 01", "slug": "produto-01", "price": 119.90, "price_with_discount": 99.90,
                 "category_ids": [%d],
                 "images": [
                   {"type": "image/png", "content": "%s"},
                   {"type": "image/png", "content": "%s"},
                   {"type": "image/png", "content": "%s"}
                 ],
                 "options": [
                   {"title": "Cor", "values": ["Azul"]},
                   {"title": "Tamanho", "radius": 4, "values": ["40/41"]}
                 ]}
                """.formatted(shoesId, PNG_BASE64, PNG_BASE64, PNG_BASE64));
        String image1Url = JsonPath.read(created, "$.images[0].content");
        String image2Url = JsonPath.read(created, "$.images[1].content");
        String image3Url = JsonPath.read(created, "$.images[2].content");

        mockMvc.perform(put("/v1/product/1").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "name": "Produto 01 atualizado",
                                  "slug": "produto-01-atualizado",
                                  "stock": 20,
                                  "description": "Descrição do produto 01 atualizado",
                                  "price": 49.9,
                                  "price_with_discount": 0,
                                  "category_ids": [%d, %d],
                                  "images": [
                                    {"type": "image/png", "content": "%s"},
                                    {"id": 2, "deleted": true},
                                    {"id": 3, "content": "%s"},
                                    {"id": 1, "content": "%s"}
                                  ],
                                  "options": [
                                    {"id": 1, "deleted": true},
                                    {"id": 2, "radius": "10px", "value": ["42/43", "44/45"]},
                                    {"title": "Tipo", "shape": "square", "type": "text", "values": ["100%% algodão", "65%% algodão"]}
                                  ]
                                }
                                """.formatted(shoesId, offersId, PNG_BASE64, JPEG_BASE64, image1Url)))
                .andExpect(status().isNoContent());

        String updated = getProduct(1);
        assertThat((String) JsonPath.read(updated, "$.name")).isEqualTo("Produto 01 atualizado");
        assertThat((Integer) JsonPath.read(updated, "$.stock")).isEqualTo(20);
        assertThat((List<Integer>) JsonPath.read(updated, "$.category_ids")).containsExactly(shoesId, offersId);

        assertThat((List<Integer>) JsonPath.read(updated, "$.images[*].id")).containsExactly(1, 3, 4);
        assertThat((String) JsonPath.read(updated, "$.images[0].content")).isEqualTo(image1Url);
        String newImage3Url = JsonPath.read(updated, "$.images[1].content");
        assertThat(newImage3Url).isNotEqualTo(image3Url).endsWith(".jpg");
        assertThat(Files.exists(fileOf(image2Url))).isFalse();
        assertThat(Files.exists(fileOf(image3Url))).isFalse();
        assertThat(Files.exists(fileOf(newImage3Url))).isTrue();

        assertThat((List<Integer>) JsonPath.read(updated, "$.options[*].id")).containsExactly(2, 3);
        assertThat((String) JsonPath.read(updated, "$.options[0].title")).isEqualTo("Tamanho");
        assertThat((Integer) JsonPath.read(updated, "$.options[0].radius")).isEqualTo(10);
        assertThat((List<String>) JsonPath.read(updated, "$.options[0].values")).containsExactly("42/43", "44/45");
        assertThat((String) JsonPath.read(updated, "$.options[1].title")).isEqualTo("Tipo");
    }

    @Test
    void categoryIdsOmitidoMantemEVazioRemoveVinculos() throws Exception {
        createProduct(fullPayload());
        String token = bearerToken();

        mockMvc.perform(put("/v1/product/1").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Produto 01", "slug": "produto-01", "price": 100, "price_with_discount": 90}
                                """))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/v1/product/1"))
                .andExpect(jsonPath("$.category_ids", hasSize(2)))
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.stock").value(10));

        mockMvc.perform(put("/v1/product/1").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Produto 01", "slug": "produto-01", "price": 100, "price_with_discount": 90,
                                 "category_ids": []}
                                """))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/v1/product/1")).andExpect(jsonPath("$.category_ids", hasSize(0)));
    }

    @Test
    void atualizacaoRejeitaImagemEOpcaoDeOutroProduto() throws Exception {
        createProduct(fullPayload());
        createProduct(fullPayload().replace("produto-01", "produto-02"));
        String token = bearerToken();

        mockMvc.perform(put("/v1/product/1").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "P", "slug": "produto-01", "price": 10, "price_with_discount": 5,
                                 "images": [{"id": 3, "deleted": true}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("images[0].id"));

        mockMvc.perform(put("/v1/product/1").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "P", "slug": "produto-01", "price": 10, "price_with_discount": 5,
                                 "options": [{"id": 3, "deleted": true}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("options[0].id"));
    }

    @Test
    void atualizacaoSemTokenOuInexistente() throws Exception {
        String payload = """
                {"name": "P", "slug": "p", "price": 10, "price_with_discount": 5}
                """;
        mockMvc.perform(put("/v1/product/1").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/v1/product/999").header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluiProdutoComArquivosEMantemCategorias() throws Exception {
        String body = createProduct(fullPayload());
        String imageUrl = JsonPath.read(body, "$.images[0].content");
        assertThat(Files.exists(fileOf(imageUrl))).isTrue();

        mockMvc.perform(delete("/v1/product/1").header("Authorization", bearerToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/product/1")).andExpect(status().isNotFound());
        assertThat(Files.exists(uploadRoot.resolve("products/1"))).isFalse();
        mockMvc.perform(get("/v1/category/{id}", shoesId)).andExpect(status().isOk());
    }

    @Test
    void exclusaoSemTokenOuInexistente() throws Exception {
        mockMvc.perform(delete("/v1/product/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/product/999").header("Authorization", bearerToken()))
                .andExpect(status().isNotFound());
    }
}
