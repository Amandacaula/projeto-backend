package br.com.digitalstore.api.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    record Sample(String priceWithDiscount, @JsonProperty("confirmPassword") String confirmPassword) {
    }

    private final Sample target = new Sample(null, null);

    @Test
    void converteCamelCaseParaSnakeCase() {
        assertThat(GlobalExceptionHandler.toJsonFieldName(target, "priceWithDiscount"))
                .isEqualTo("price_with_discount");
    }

    @Test
    void respeitaJsonProperty() {
        assertThat(GlobalExceptionHandler.toJsonFieldName(target, "confirmPassword"))
                .isEqualTo("confirmPassword");
    }

    @Test
    void converteCaminhosAninhados() {
        assertThat(GlobalExceptionHandler.toJsonFieldName(target, "categoryIds[0]"))
                .isEqualTo("category_ids[0]");
        assertThat(GlobalExceptionHandler.toJsonFieldName(target, "options[1].priceWithDiscount"))
                .isEqualTo("options[1].price_with_discount");
    }
}
