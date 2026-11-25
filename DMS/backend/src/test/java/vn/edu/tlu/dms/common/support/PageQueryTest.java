package vn.edu.tlu.dms.common.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import vn.edu.tlu.dms.common.exception.BusinessException;

import java.util.List;
import java.util.Set;

/** Kiểm tra quy tắc phân trang và tie-breaker cột id của {@link PageQuery}. */
class PageQueryTest {

    @Test
    void addsIdTieBreakerWhenIdIsAllowed() {
        var pageable = PageQuery.of(0, 20, null, Set.of("id", "code"), "code,asc");

        assertThat(pageable.getSort())
                .containsExactly(Sort.Order.asc("code"), Sort.Order.asc("id"));
    }

    @Test
    void omitsIdTieBreakerWhenIdIsNotInAllowedColumns() {
        var pageable = PageQuery.of(0, 20, null, Set.of("code", "name"), "code,asc");

        assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("code"));
    }

    @Test
    void keepsExplicitSortWithoutAddingMissingIdColumn() {
        var pageable =
                PageQuery.of(1, 10, List.of("name,desc"), Set.of("code", "name"), "code,asc");

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("name"));
    }

    @Test
    void rejectsDisallowedSortField() {
        assertThatThrownBy(() -> PageQuery.of(0, 20, List.of("id,asc"), Set.of("code"), "code,asc"))
                .isInstanceOf(BusinessException.class);
    }
}
