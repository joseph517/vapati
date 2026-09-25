package com.vaPaTi.vaPaTi.validation;

import com.vaPaTi.vaPaTi.exception.MessageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

// Shared page/size/sort check for paginated endpoints; each controller passes its own sortBy whitelist
@Service
public class SortValidationService {

    private static final int MAX_PAGE_SIZE = 100;

    public Pageable validateAndGetPageable(int page, int size, String sortBy, String sortDirection,
                                           List<String> allowedSortFields) {
        if (page < 0) {
            throw new MessageException("Page must be greater than or equal to 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new MessageException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }

        if (!allowedSortFields.contains(sortBy)) {
            throw new MessageException("Invalid sortBy '" + sortBy + "'. Allowed: " + String.join(", ", allowedSortFields));
        }

        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new MessageException("Invalid sortDirection '" + sortDirection + "'. Allowed: asc, desc");
        }

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
