package br.com.digitalstore.api.exception;

import br.com.digitalstore.api.dto.common.ErrorResponse;

import java.util.List;

public class BadRequestException extends RuntimeException {

    private final List<ErrorResponse.FieldError> errors;

    public BadRequestException(String message) {
        this(message, List.of());
    }

    public BadRequestException(String message, List<ErrorResponse.FieldError> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public static BadRequestException forField(String field, String message) {
        return new BadRequestException(message, List.of(new ErrorResponse.FieldError(field, message)));
    }

    public List<ErrorResponse.FieldError> getErrors() {
        return errors;
    }
}
