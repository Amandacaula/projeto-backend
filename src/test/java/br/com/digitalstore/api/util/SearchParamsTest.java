package br.com.digitalstore.api.util;

import br.com.digitalstore.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchParamsTest {

    @Test
    void aplicaPadroes() {
        SearchParams params = SearchParams.of(null, null);
        assertThat(params.limit()).isEqualTo(12);
        assertThat(params.page()).isEqualTo(1);
    }

    @Test
    void converteParaPageableBaseZero() {
        Pageable pageable = SearchParams.of(10, 3).toPageable(Sort.by("id"));
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void limitMenosUmIgnoraPage() {
        SearchParams params = SearchParams.of(-1, 7);
        assertThat(params.isUnlimited()).isTrue();
        assertThat(params.page()).isEqualTo(1);
        assertThat(params.toPageable(Sort.by("id")).isUnpaged()).isTrue();
    }

    @Test
    void rejeitaValoresInvalidos() {
        assertThatThrownBy(() -> SearchParams.of(0, 1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SearchParams.of(-2, 1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SearchParams.of(10, 0)).isInstanceOf(BadRequestException.class);
    }
}
