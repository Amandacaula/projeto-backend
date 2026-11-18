package br.com.digitalstore.api.service;

import br.com.digitalstore.api.config.AppProperties;
import br.com.digitalstore.api.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    static final int MAX_BYTES = 5 * 1024 * 1024;

    private final Path root;
    private final String baseUrl;

    public ImageStorageService(AppProperties properties) {
        this.root = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
        this.baseUrl = properties.baseUrl().replaceAll("/+$", "");
    }

    public Path root() {
        return root;
    }

    public String save(Integer productId, String mimeType, String content, String field) {
        ImageFormat declared = null;
        if (mimeType != null) {
            declared = ImageFormat.fromMimeType(mimeType).orElseThrow(() -> BadRequestException.forField(
                    field + ".type", "Tipo de imagem não suportado: " + mimeType
                            + ". Aceitos: " + ImageFormat.acceptedMimeTypes()));
        }

        byte[] bytes = decode(content, field);
        if (bytes.length > MAX_BYTES) {
            throw BadRequestException.forField(field + ".content", "A imagem deve ter no máximo 5 MB");
        }
        ImageFormat detected = ImageFormat.detect(bytes).orElseThrow(() -> BadRequestException.forField(
                field + ".content", "O conteúdo enviado não é uma imagem suportada"));
        if (declared != null && declared != detected) {
            throw BadRequestException.forField(field + ".type",
                    "O conteúdo da imagem não corresponde ao tipo " + mimeType);
        }

        String relativePath = "products/" + productId + "/" + UUID.randomUUID() + "." + detected.extension();
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar imagem " + relativePath, e);
        }
        deleteIfTransactionFails(relativePath);
        return relativePath;
    }

    public void deleteAfterCommit(String relativePath) {
        runAfterCommit(() -> delete(relativePath));
    }

    public void deleteProductFolderAfterCommit(Integer productId) {
        runAfterCommit(() -> {
            try {
                FileSystemUtils.deleteRecursively(resolve("products/" + productId));
            } catch (IOException e) {
                log.warn("Não foi possível remover a pasta de imagens do produto {}", productId, e);
            }
        });
    }

    public String toUrl(String relativePath) {
        return baseUrl + "/media/" + relativePath;
    }

    void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("Não foi possível remover a imagem {}", relativePath, e);
        }
    }

    private static byte[] decode(String content, String field) {
        if (content == null || content.isBlank()) {
            throw BadRequestException.forField(field + ".content", "O campo content é obrigatório");
        }
        String data = content.trim();
        if (data.startsWith("data:")) {
            int comma = data.indexOf(',');
            data = comma >= 0 ? data.substring(comma + 1) : "";
        }
        try {
            return Base64.getDecoder().decode(data.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw BadRequestException.forField(field + ".content", "Imagem em base64 inválida");
        }
    }

    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Caminho fora do diretório de uploads: " + relativePath);
        }
        return path;
    }

    private void deleteIfTransactionFails(String relativePath) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        delete(relativePath);
                    }
                }
            });
        }
    }

    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
