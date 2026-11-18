package br.com.digitalstore.api.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum ImageFormat {
    PNG("png", Set.of("image/png")),
    JPEG("jpg", Set.of("image/jpeg", "image/jpg")),
    WEBP("webp", Set.of("image/webp")),
    GIF("gif", Set.of("image/gif"));

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private final String extension;
    private final Set<String> mimeTypes;

    ImageFormat(String extension, Set<String> mimeTypes) {
        this.extension = extension;
        this.mimeTypes = mimeTypes;
    }

    public String extension() {
        return extension;
    }

    public static String acceptedMimeTypes() {
        return "image/png, image/jpeg, image/jpg, image/webp, image/gif";
    }

    public static Optional<ImageFormat> fromMimeType(String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(f -> f.mimeTypes.contains(normalized)).findFirst();
    }

    public static Optional<ImageFormat> detect(byte[] bytes) {
        if (startsWith(bytes, PNG_SIGNATURE)) {
            return Optional.of(PNG);
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if (startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII))) {
            return Optional.of(GIF);
        }
        if (bytes.length >= 12 && startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
