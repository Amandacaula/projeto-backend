package br.com.digitalstore.api.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(Instant timestamp, int status, String error, String message,
                            String path, List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(HttpStatusCode status, String message, String path, List<FieldError> errors) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
        List<FieldError> fieldErrors = (errors == null || errors.isEmpty()) ? null : List.copyOf(errors);
        return new ErrorResponse(Instant.now(), status.value(), reason, message, path, fieldErrors);
    }
}
