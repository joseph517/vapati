package com.vaPaTi.vaPaTi.utils;

import com.vaPaTi.vaPaTi.exception.MessageException;
import org.jetbrains.annotations.NotNull;

public class ValidationUtils {

    private ValidationUtils() {}

    public static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new MessageException(fieldName + " cannot be empty");
        }
    }

    public static void validateIfPresent(String value, String fieldName) {
        if (value != null) {
            validateNotBlank(value, fieldName);
        }
    }

    public static void validateAtLeastOneFieldPresent(Object @NotNull ... fields) {
        for (Object field : fields) {
            if (field != null) {
                return;
            }
        }
        throw new MessageException("At least one field must be provided for update");
    }
}
