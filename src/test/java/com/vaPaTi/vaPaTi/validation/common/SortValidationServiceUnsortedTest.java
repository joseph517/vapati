package com.vaPaTi.vaPaTi.validation.common;

import com.vaPaTi.vaPaTi.exception.MessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SortValidationService - Unsorted pagination")
class SortValidationServiceUnsortedTest {

    private SortValidationService sortValidationService;

    @BeforeEach
    void setUp() {
        sortValidationService = new SortValidationService();
    }

    @Nested
    @DisplayName("Valid values")
    class ValidValuesTests {

        @Test
        @DisplayName("Returns the Pageable with the requested page and size and no sort")
        void shouldReturnUnsortedPageable() {
            Pageable pageable = sortValidationService.validateAndGetPageable(2, 20);

            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(20);
            assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
        }

        @Test
        @DisplayName("Accepts the lower limits page 0 and size 1")
        void shouldAcceptLowerLimits() {
            Pageable pageable = sortValidationService.validateAndGetPageable(0, 1);

            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(1);
            assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
        }

        @Test
        @DisplayName("Accepts the maximum size 100")
        void shouldAcceptMaxSize() {
            Pageable pageable = sortValidationService.validateAndGetPageable(0, 100);

            assertThat(pageable.getPageSize()).isEqualTo(100);
            assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
        }
    }

    @Nested
    @DisplayName("Invalid values")
    class InvalidValuesTests {

        @Test
        @DisplayName("Rejects a negative page")
        void shouldRejectNegativePage() {
            assertThatThrownBy(() -> sortValidationService.validateAndGetPageable(-1, 10))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Page must be greater than or equal to 0");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 101})
        @DisplayName("Rejects a size outside 1..100")
        void shouldRejectSizeOutOfRange(int size) {
            assertThatThrownBy(() -> sortValidationService.validateAndGetPageable(0, size))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Size must be between 1 and 100");
        }
    }
}
