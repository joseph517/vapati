package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.exception.MessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SortValidationService - Unit Tests")
class SortValidationServiceTest {

    private static final List<String> ALLOWED = List.of("createdAt", "status");

    private SortValidationService sortValidationService;

    @BeforeEach
    void setUp() {
        sortValidationService = new SortValidationService();
    }

    @Nested
    @DisplayName("Valid values")
    class ValidValuesTests {

        @Test
        @DisplayName("Returns the Pageable with the requested page, size, field and direction")
        void shouldReturnPageable() {
            Pageable pageable = sortValidationService.validateAndGetPageable(2, 20, "status", "asc", ALLOWED);

            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(20);
            assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "status"));
        }

        @Test
        @DisplayName("Accepts desc")
        void shouldAcceptDesc() {
            Pageable pageable = sortValidationService.validateAndGetPageable(0, 10, "createdAt", "desc", ALLOWED);

            assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("Accepts ASC and DESC in uppercase")
        void shouldAcceptUppercaseDirection() {
            Pageable ascending = sortValidationService.validateAndGetPageable(0, 10, "createdAt", "ASC", ALLOWED);
            Pageable descending = sortValidationService.validateAndGetPageable(0, 10, "createdAt", "DESC", ALLOWED);

            assertThat(ascending.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
            assertThat(descending.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 100})
        @DisplayName("Accepts the size bounds 1 and 100")
        void shouldAcceptSizeBounds(int size) {
            Pageable pageable = sortValidationService.validateAndGetPageable(0, size, "createdAt", "desc", ALLOWED);

            assertThat(pageable.getPageSize()).isEqualTo(size);
        }

        @Test
        @DisplayName("Accepts page 0")
        void shouldAcceptFirstPage() {
            Pageable pageable = sortValidationService.validateAndGetPageable(0, 10, "createdAt", "desc", ALLOWED);

            assertThat(pageable.getPageNumber()).isZero();
        }
    }

    @Nested
    @DisplayName("Invalid values")
    class InvalidValuesTests {

        @Test
        @DisplayName("Rejects page -1")
        void shouldRejectNegativePage() {
            assertThatThrownBy(() -> sortValidationService.validateAndGetPageable(-1, 10, "createdAt", "desc", ALLOWED))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Page must be greater than or equal to 0");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 101})
        @DisplayName("Rejects size 0 and 101")
        void shouldRejectSizeOutOfBounds(int size) {
            assertThatThrownBy(() -> sortValidationService.validateAndGetPageable(0, size, "createdAt", "desc", ALLOWED))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Size must be between 1 and 100");
        }

        @Test
        @DisplayName("Rejects a sortBy outside the whitelist and lists the allowed fields")
        void shouldRejectUnknownSortBy() {
            assertThatThrownBy(() -> sortValidationService.validateAndGetPageable(0, 10, "noExiste", "desc", ALLOWED))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Invalid sortBy 'noExiste'. Allowed: createdAt, status");
        }

        @Test
        @DisplayName("Compares sortBy exactly, without ignoring case")
        void shouldRejectSortByWithDifferentCase() {
            assertThatThrownBy(() -> sortValidationService.validateAndGetPageable(0, 10, "CreatedAt", "desc", ALLOWED))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Invalid sortBy 'CreatedAt'. Allowed: createdAt, status");
        }

        @Test
        @DisplayName("Rejects a nested property path such as userInfo.password")
        void shouldRejectNestedPath() {
            assertThatThrownBy(() -> sortValidationService.validateAndGetPageable(
                    0, 10, "userInfo.password", "asc", List.of("id", "createdAt")))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Invalid sortBy 'userInfo.password'. Allowed: id, createdAt");
        }

        @Test
        @DisplayName("Rejects a sortDirection other than asc or desc")
        void shouldRejectUnknownSortDirection() {
            assertThatThrownBy(() -> sortValidationService.validateAndGetPageable(0, 10, "createdAt", "banana", ALLOWED))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Invalid sortDirection 'banana'. Allowed: asc, desc");
        }
    }
}
