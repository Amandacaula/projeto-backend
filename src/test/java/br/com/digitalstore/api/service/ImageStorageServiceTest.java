package br.com.digitalstore.api.service;

import br.com.digitalstore.api.config.AppProperties;
import br.com.digitalstore.api.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final String PNG_BASE64 = Base64.getEncoder().encodeToString(PNG);

    @TempDir
    Path tempDir;

    private ImageStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new ImageStorageService(new AppProperties("http://localhost:8080/", tempDir.toString(),
                new AppProperties.Jwt("x".repeat(32), 60)));
    }

    @Test
    void gravaArquivoComNomeGeradoEDevolveCaminhoRelativo() throws Exception {
        String path = storage.save(7, "image/png", PNG_BASE64, "images[0]");

        assertThat(path).matches("products/7/[0-9a-f-]{36}\\.png");
        assertThat(Files.readAllBytes(tempDir.resolve(path))).isEqualTo(PNG);
        assertThat(storage.toUrl(path)).isEqualTo("http://localhost:8080/media/" + path);
    }

    @Test
    void aceitaDataUriEDetectaFormatoSemType() {
        String path = storage.save(1, null, "data:image/png;base64," + PNG_BASE64, "images[0]");
        assertThat(path).endsWith(".png");
    }

    @Test
    void rejeitaBase64InvalidoTipoNaoSuportadoEConteudoQueNaoEImagem() {
        assertThatThrownBy(() -> storage.save(1, "image/png", "@@@", "images[0]"))
                .isInstanceOf(BadRequestException.class).hasMessage("Imagem em base64 inválida");
        assertThatThrownBy(() -> storage.save(1, "image/bmp", PNG_BASE64, "images[0]"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("não suportado");
        assertThatThrownBy(() -> storage.save(1, "image/png", Base64.getEncoder().encodeToString("texto".getBytes()), "images[0]"))
                .isInstanceOf(BadRequestException.class).hasMessage("O conteúdo enviado não é uma imagem suportada");
        assertThatThrownBy(() -> storage.save(1, "image/jpeg", PNG_BASE64, "images[0]"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("não corresponde");
    }

    @Test
    void foraDeTransacaoApagaImediatamente() {
        String path = storage.save(1, "image/png", PNG_BASE64, "images[0]");
        storage.deleteAfterCommit(path);
        assertThat(Files.exists(tempDir.resolve(path))).isFalse();
    }

    @Test
    void recusaCaminhoForaDaPastaDeUploads() {
        assertThatThrownBy(() -> storage.deleteAfterCommit("../fora.png"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
