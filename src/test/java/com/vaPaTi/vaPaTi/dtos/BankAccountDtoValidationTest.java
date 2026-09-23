package com.vaPaTi.vaPaTi.dtos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertSingleViolation;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertValid;
import static com.vaPaTi.vaPaTi.dtos.DtoValidation.assertViolationsOn;

@DisplayName("CreateBankAccountDTO - Bean Validation")
class BankAccountDtoValidationTest {

    private CreateBankAccountDTO dto;

    @BeforeEach
    void setUp() {
        dto = CreateBankAccountDTO.builder()
                .bankName("Banco Nación")
                .accountNumber("0110012340000012345678")
                .accountType("SAVINGS")
                .accountHolder("John Doe")
                .build();
    }

    static Stream<Arguments> fields() {
        return Stream.of(
                Arguments.of("bankName", 100, (BiConsumer<CreateBankAccountDTO, String>) CreateBankAccountDTO::setBankName),
                Arguments.of("accountNumber", 50, (BiConsumer<CreateBankAccountDTO, String>) CreateBankAccountDTO::setAccountNumber),
                Arguments.of("accountType", 50, (BiConsumer<CreateBankAccountDTO, String>) CreateBankAccountDTO::setAccountType),
                Arguments.of("accountHolder", 100, (BiConsumer<CreateBankAccountDTO, String>) CreateBankAccountDTO::setAccountHolder)
        );
    }

    @Test
    @DisplayName("A valid DTO has no violations; userId has no constraints (P04)")
    void validDtoHasNoViolations() {
        assertValid(dto);
    }

    @Test
    @DisplayName("An empty DTO has a violation on each field")
    void emptyDto() {
        assertViolationsOn(new CreateBankAccountDTO(), "bankName", "accountNumber", "accountType", "accountHolder");
    }

    @ParameterizedTest(name = "missing {0}")
    @MethodSource("fields")
    @DisplayName("Each field is required")
    void missingField(String field, int max, BiConsumer<CreateBankAccountDTO, String> setter) {
        setter.accept(dto, null);
        assertSingleViolation(dto, field);
    }

    @ParameterizedTest(name = "blank {0}")
    @MethodSource("fields")
    @DisplayName("Each field can't be blank")
    void blankField(String field, int max, BiConsumer<CreateBankAccountDTO, String> setter) {
        setter.accept(dto, "   ");
        assertSingleViolation(dto, field);
    }

    @ParameterizedTest(name = "{0} over {1} characters")
    @MethodSource("fields")
    @DisplayName("Each field rejects one character over the UpdateBankAccountDTO limit")
    void fieldTooLong(String field, int max, BiConsumer<CreateBankAccountDTO, String> setter) {
        setter.accept(dto, "a".repeat(max + 1));
        assertSingleViolation(dto, field);
    }

    @ParameterizedTest(name = "{0} with {1} characters")
    @MethodSource("fields")
    @DisplayName("Each field accepts exactly its limit")
    void fieldAtLimit(String field, int max, BiConsumer<CreateBankAccountDTO, String> setter) {
        setter.accept(dto, "a".repeat(max));
        assertValid(dto);
    }
}
