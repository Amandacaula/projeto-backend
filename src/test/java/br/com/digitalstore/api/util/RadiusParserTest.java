package br.com.digitalstore.api.util;

import br.com.digitalstore.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RadiusParserTest {

    @Test
    void aceitaNumeroETextoComOuSemPx() {
        assertThat(RadiusParser.parse(4, "radius")).isEqualTo(4);
        assertThat(RadiusParser.parse(4.0, "radius")).isEqualTo(4);
        assertThat(RadiusParser.parse("4", "radius")).isEqualTo(4);
        assertThat(RadiusParser.parse("4px", "radius")).isEqualTo(4);
        assertThat(RadiusParser.parse(" 10 PX ", "radius")).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "4.5px", "", "10em"})
    void rejeitaTextoInvalido(String value) {
        assertThatThrownBy(() -> RadiusParser.parse(value, "options[0].radius"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejeitaNumeroNegativoOuFracionario() {
        assertThatThrownBy(() -> RadiusParser.parse(-1, "radius")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> RadiusParser.parse(4.5, "radius")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> RadiusParser.parse(true, "radius")).isInstanceOf(BadRequestException.class);
    }
}
