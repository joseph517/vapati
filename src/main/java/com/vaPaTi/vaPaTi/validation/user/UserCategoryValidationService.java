package com.vaPaTi.vaPaTi.validation.user;

import com.vaPaTi.vaPaTi.entity.category.Category;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserCategory;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// User categories: limit per user, existence of the requested ids, and creation and replacement of the relations
@Service
@RequiredArgsConstructor
public class UserCategoryValidationService {

    private static final Number MAX_CATEGORIES_PER_USER = 6;
    private final CategoryRepository categoryRepository;

    public void validateCategoryLimit( List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            throw new MessageException("User must have at least 1 category");
        }

        if (categoryIds.size() > MAX_CATEGORIES_PER_USER.intValue()) {
            throw new MessageException("User cannot have more than " + MAX_CATEGORIES_PER_USER + " categories");
        }
    }

    public @NotNull List<Category> processCategories(List<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);

        Set<Long> foundIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = categoryIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Categories not found: " + missingIds);
        }

        return categories;
    }

    /**
     * Create a list of {@link UserCategory} objects from a list of categories
     * and a user, and set the user's categories.
     *
     * @param user    the user to create the categories for
     * @param categories the categories to create the relations for
     */
    public void createUserCategoryRelations(User user, @NotNull List<Category> categories) {
        Set<UserCategory> userCategories = new HashSet<>();

        for (Category category : categories) {
            UserCategory userCategory = new UserCategory();
            userCategory.setUser(user);
            userCategory.setCategory(category);
            userCategories.add(userCategory);
        }

        user.setUserCategories(userCategories);
    }

    public void updateUserCategories(@NotNull User user, @NotNull List<Category> newCategories) {
        Set<Long> newCategoryIds = newCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        // Remove categories that are not in the new list
        user.getUserCategories().removeIf(uc -> {
            if (!newCategoryIds.contains(uc.getCategory().getId())) {
                uc.setUser(null); // Desasociar
                return true;
            }
            return false;
        });

        // Add new categories
        Set<Long> existingIds = user.getUserCategories().stream()
                .map(uc -> uc.getCategory().getId())
                .collect(Collectors.toSet());

        newCategories.stream()
                .filter(c -> !existingIds.contains(c.getId()))
                .forEach(c -> {
                    UserCategory uc = new UserCategory();
                    uc.setUser(user);
                    uc.setCategory(c);
                    user.getUserCategories().add(uc);
                });
    }
}
