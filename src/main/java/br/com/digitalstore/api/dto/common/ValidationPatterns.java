package br.com.digitalstore.api.dto.common;

public final class ValidationPatterns {

    public static final String SLUG = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    public static final String SLUG_MESSAGE = "Slug deve conter apenas letras minúsculas, números e hífens";

    private ValidationPatterns() {
    }
}
