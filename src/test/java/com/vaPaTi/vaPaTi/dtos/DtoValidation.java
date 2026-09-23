package com.vaPaTi.vaPaTi.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Shared Bean Validation helpers for the DTO tests (plain Hibernate Validator, no Spring context)
final class DtoValidation {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private DtoValidation() {
    }

    static <T> Set<ConstraintViolation<T>> validate(T dto) {
        return VALIDATOR.validate(dto);
    }

    static <T> void assertValid(T dto) {
        assertThat(validate(dto)).isEmpty();
    }

    static <T> void assertSingleViolation(T dto, String field) {
        Set<ConstraintViolation<T>> violations = validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString(field);
    }

    static <T> void assertViolationsOn(T dto, String... fields) {
        assertThat(validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder(fields);
    }
}
