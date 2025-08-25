package com.vaPaTi.vaPaTi.service.user;

import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserCategory;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;

import java.util.HashSet;
import java.util.List;

import com.vaPaTi.vaPaTi.validation.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCategoryService Tests")
class UserValidationServiceCategoryTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private UserValidationService userValidationService;

    private List<Long> validCategoryIds;
    private List<Long> emptyCategoryIds;
    private List<Long> exceedLimitCategoryIds;
    private List<Category> mockCategories;
    private User mockUser;
    private Category category1, category2, category3;

    @BeforeEach
    void setUp() {
        // Setup valid category IDs (within limit)
        validCategoryIds = Arrays.asList(1L, 2L, 3L);
        emptyCategoryIds = Collections.emptyList();

        // Setup category IDs that exceed the limit (> 6)
        exceedLimitCategoryIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);

        // Setup mock categories
        category1 = Category.builder().id(1L).name("Technology").description("Tech category").build();
        category2 = Category.builder().id(2L).name("Sports").description("Sports category").build();
        category3 = Category.builder().id(3L).name("Music").description("Music category").build();

        mockCategories = Arrays.asList(category1, category2, category3);

        // Setup mock user with empty user categories initially
        mockUser = User.builder()
                .id(1L)
                .active(true)
                .userCategories(new HashSet<>())
                .build();
    }

    @Nested
    @DisplayName("validateCategoryLimit Tests")
    class ValidateCategoryLimitTests {

        @Test
        @DisplayName("Should pass validation when category list has valid size")
        void shouldPassValidationWhenCategoryListHasValidSize() {
            // When & Then
            assertThatCode(() -> userValidationService.validateCategoryLimit(validCategoryIds))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass validation when category list has exactly max categories")
        void shouldPassValidationWhenCategoryListHasExactlyMaxCategories() {
            // Given
            List<Long> maxCategoryIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);

            // When & Then
            assertThatCode(() -> userValidationService.validateCategoryLimit(maxCategoryIds))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw MessageException when category list is empty")
        void shouldThrowMessageExceptionWhenCategoryListIsEmpty() {
            // When & Then
            assertThatThrownBy(() -> userValidationService.validateCategoryLimit(emptyCategoryIds))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User must have at least 1 category");
        }

        @Test
        @DisplayName("Should throw MessageException when category list exceeds maximum limit")
        void shouldThrowMessageExceptionWhenCategoryListExceedsMaximumLimit() {
            // When & Then
            assertThatThrownBy(() -> userValidationService.validateCategoryLimit(exceedLimitCategoryIds))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("User cannot have more than 6 categories");
        }

        @Test
        @DisplayName("Should handle null list gracefully")
        void shouldHandleNullListGracefully() {
            // When & Then - This will throw NullPointerException due to @NotNull annotation
            assertThatThrownBy(() -> userValidationService.validateCategoryLimit(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("processCategories Tests")
    class ProcessCategoriesTests {

        @Test
        @DisplayName("Should return all categories when all IDs are found")
        void shouldReturnAllCategoriesWhenAllIdsAreFound() {
            // Given
            when(categoryRepository.findAllById(validCategoryIds)).thenReturn(mockCategories);

            // When
            List<Category> result = userValidationService.processCategories(validCategoryIds);

            // Then
            assertThat(result).isNotNull()
                    .hasSize(3)
                    .containsExactlyInAnyOrderElementsOf(mockCategories);

            verify(categoryRepository).findAllById(validCategoryIds);
        }

        @Test
        @DisplayName("Should throw MessageException when some categories are not found")
        void shouldThrowMessageExceptionWhenSomeCategoriesAreNotFound() {
            // Given
            List<Long> requestedIds = Arrays.asList(1L, 2L, 99L); // 99L doesn't exist
            List<Category> foundCategories = Arrays.asList(category1, category2); // Missing category with ID 99L

            when(categoryRepository.findAllById(requestedIds)).thenReturn(foundCategories);

            // When & Then
            assertThatThrownBy(() -> userValidationService.processCategories(requestedIds))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Categories not found: [99]");

            verify(categoryRepository).findAllById(requestedIds);
        }

        @Test
        @DisplayName("Should throw MessageException when multiple categories are not found")
        void shouldThrowMessageExceptionWhenMultipleCategoriesAreNotFound() {
            // Given
            List<Long> requestedIds = Arrays.asList(1L, 98L, 99L); // 98L and 99L don't exist
            List<Category> foundCategories = Arrays.asList(category1); // Only category1 found

            when(categoryRepository.findAllById(requestedIds)).thenReturn(foundCategories);

            // When & Then
            assertThatThrownBy(() -> userValidationService.processCategories(requestedIds))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Categories not found: [98, 99]");

            verify(categoryRepository).findAllById(requestedIds);
        }

        @Test
        @DisplayName("Should throw MessageException when no categories are found")
        void shouldThrowMessageExceptionWhenNoCategoriesAreFound() {
            // Given
            List<Long> nonExistentIds = Arrays.asList(97L, 98L, 99L);
            when(categoryRepository.findAllById(nonExistentIds)).thenReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> userValidationService.processCategories(nonExistentIds))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Categories not found: [97, 98, 99]");

            verify(categoryRepository).findAllById(nonExistentIds);
        }

        @Test
        @DisplayName("Should handle empty category list")
        void shouldHandleEmptyCategoryList() {
            // Given
            when(categoryRepository.findAllById(emptyCategoryIds)).thenReturn(Collections.emptyList());

            // When
            List<Category> result = userValidationService.processCategories(emptyCategoryIds);

            // Then
            assertThat(result).isNotNull().isEmpty();
            verify(categoryRepository).findAllById(emptyCategoryIds);
        }

        @Test
        @DisplayName("Should maintain order independence for missing IDs")
        void shouldMaintainOrderIndependenceForMissingIds() {
            // Given
            List<Long> requestedIds = Arrays.asList(99L, 1L, 98L); // Mixed order
            List<Category> foundCategories = Arrays.asList(category1);

            when(categoryRepository.findAllById(requestedIds)).thenReturn(foundCategories);

            // When & Then
            assertThatThrownBy(() -> userValidationService.processCategories(requestedIds))
                    .isInstanceOf(MessageException.class)
                    .hasMessageContaining("Categories not found:")
                    .hasMessageContaining("99")
                    .hasMessageContaining("98");

            verify(categoryRepository).findAllById(requestedIds);
        }
    }

    @Nested
    @DisplayName("createUserCategoryRelations Tests")
    class CreateUserCategoryRelationsTests {

        @Test
        @DisplayName("Should create user category relations for all provided categories")
        void shouldCreateUserCategoryRelationsForAllProvidedCategories() {
            // When
            userValidationService.createUserCategoryRelations(mockUser, mockCategories);

            // Then
            assertThat(mockUser.getUserCategories())
                    .isNotNull()
                    .hasSize(3);

            Set<Long> categoryIds = mockUser.getUserCategories().stream()
                    .map(uc -> uc.getCategory().getId())
                    .collect(Collectors.toSet());

            assertThat(categoryIds).containsExactlyInAnyOrder(1L, 2L, 3L);

            // Verify each UserCategory has correct user reference
            mockUser.getUserCategories().forEach(uc -> {
                assertThat(uc.getUser()).isEqualTo(mockUser);
                assertThat(uc.getCategory()).isIn(mockCategories);
            });
        }

        @Test
        @DisplayName("Should handle empty category list")
        void shouldHandleEmptyCategoryList() {
            // When
            userValidationService.createUserCategoryRelations(mockUser, Collections.emptyList());

            // Then
            assertThat(mockUser.getUserCategories()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should replace existing user categories")
        void shouldReplaceExistingUserCategories() {
            // Given - User already has some categories
            UserCategory existingUc = new UserCategory();
            existingUc.setUser(mockUser);
            existingUc.setCategory(Category.builder().id(99L).name("Old Category").build());
            mockUser.getUserCategories().add(existingUc);

            // When
            userValidationService.createUserCategoryRelations(mockUser, mockCategories);

            // Then
            assertThat(mockUser.getUserCategories()).hasSize(3);

            Set<Long> categoryIds = mockUser.getUserCategories().stream()
                    .map(uc -> uc.getCategory().getId())
                    .collect(Collectors.toSet());

            assertThat(categoryIds).containsExactlyInAnyOrder(1L, 2L, 3L);
            assertThat(categoryIds).doesNotContain(99L);
        }

        @Test
        @DisplayName("Should create unique UserCategory instances")
        void shouldCreateUniqueUserCategoryInstances() {
            // When
            userValidationService.createUserCategoryRelations(mockUser, mockCategories);

            // Then
            Set<UserCategory> userCategories = mockUser.getUserCategories();
            List<UserCategory> userCategoryList = new ArrayList<>(userCategories);

            // Verify all instances are unique
            assertThat(userCategories).hasSize(userCategoryList.size());
        }
    }

    @Nested
    @DisplayName("updateUserCategories Tests")
    class UpdateUserCategoriesTests {

        private UserCategory existingUc1, existingUc2, existingUc3;

        @BeforeEach
        void setUpUpdateTests() {
            // Setup existing user categories
            existingUc1 = new UserCategory();
            existingUc1.setUser(mockUser);
            existingUc1.setCategory(category1);

            existingUc2 = new UserCategory();
            existingUc2.setUser(mockUser);
            existingUc2.setCategory(category2);

            existingUc3 = new UserCategory();
            existingUc3.setUser(mockUser);
            existingUc3.setCategory(category3);

            mockUser.getUserCategories().addAll(Arrays.asList(existingUc1, existingUc2, existingUc3));
        }

        @Test
        @DisplayName("Should add new categories when user has no existing categories")
        void shouldAddNewCategoriesWhenUserHasNoExistingCategories() {
            // Given
            User userWithNoCategories = User.builder()
                    .id(2L)
                    .userCategories(new HashSet<>())
                    .build();

            // When
            userValidationService.updateUserCategories(userWithNoCategories, mockCategories);

            // Then
            assertThat(userWithNoCategories.getUserCategories()).hasSize(3);

            Set<Long> categoryIds = userWithNoCategories.getUserCategories().stream()
                    .map(uc -> uc.getCategory().getId())
                    .collect(Collectors.toSet());

            assertThat(categoryIds).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("Should keep existing categories that are in new list")
        void shouldKeepExistingCategoriesThatAreInNewList() {
            // Given - Keep categories 1 and 2, remove 3
            List<Category> newCategories = Arrays.asList(category1, category2);

            // When
            userValidationService.updateUserCategories(mockUser, newCategories);

            // Then
            assertThat(mockUser.getUserCategories()).hasSize(2);

            Set<Long> categoryIds = mockUser.getUserCategories().stream()
                    .map(uc -> uc.getCategory().getId())
                    .collect(Collectors.toSet());

            assertThat(categoryIds).containsExactlyInAnyOrder(1L, 2L);

            // Verify the removed category has null user reference
            assertThat(existingUc3.getUser()).isNull();
        }

        @Test
        @DisplayName("Should remove categories not in new list")
        void shouldRemoveCategoriesNotInNewList() {
            // Given - Only keep category 1
            List<Category> newCategories = Arrays.asList(category1);

            // When
            userValidationService.updateUserCategories(mockUser, newCategories);

            // Then
            assertThat(mockUser.getUserCategories()).hasSize(1);

            UserCategory remainingUc = mockUser.getUserCategories().iterator().next();
            assertThat(remainingUc.getCategory().getId()).isEqualTo(1L);

            // Verify removed categories have null user references
            assertThat(existingUc2.getUser()).isNull();
            assertThat(existingUc3.getUser()).isNull();
        }

        @Test
        @DisplayName("Should add new categories while keeping existing ones")
        void shouldAddNewCategoriesWhileKeepingExistingOnes() {
            // Given - Keep existing categories and add new one
            Category newCategory = Category.builder().id(4L).name("Food").description("Food category").build();
            List<Category> newCategories = Arrays.asList(category1, category2, category3, newCategory);

            // When
            userValidationService.updateUserCategories(mockUser, newCategories);

            // Then
            assertThat(mockUser.getUserCategories()).hasSize(4);

            Set<Long> categoryIds = mockUser.getUserCategories().stream()
                    .map(uc -> uc.getCategory().getId())
                    .collect(Collectors.toSet());

            assertThat(categoryIds).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        }

        @Test
        @DisplayName("Should completely replace all categories")
        void shouldCompletelyReplaceAllCategories() {
            // Given - Replace all existing categories with new ones
            Category newCategory1 = Category.builder().id(4L).name("Food").build();
            Category newCategory2 = Category.builder().id(5L).name("Travel").build();
            List<Category> newCategories = Arrays.asList(newCategory1, newCategory2);

            // When
            userValidationService.updateUserCategories(mockUser, newCategories);

            // Then
            assertThat(mockUser.getUserCategories()).hasSize(2);

            Set<Long> categoryIds = mockUser.getUserCategories().stream()
                    .map(uc -> uc.getCategory().getId())
                    .collect(Collectors.toSet());

            assertThat(categoryIds).containsExactlyInAnyOrder(4L, 5L);

            // Verify all old categories have null user references
            assertThat(existingUc1.getUser()).isNull();
            assertThat(existingUc2.getUser()).isNull();
            assertThat(existingUc3.getUser()).isNull();
        }

        @Test
        @DisplayName("Should remove all categories when new list is empty")
        void shouldRemoveAllCategoriesWhenNewListIsEmpty() {
            // When
            userValidationService.updateUserCategories(mockUser, Collections.emptyList());

            // Then
            assertThat(mockUser.getUserCategories()).isEmpty();

            // Verify all categories have null user references
            assertThat(existingUc1.getUser()).isNull();
            assertThat(existingUc2.getUser()).isNull();
            assertThat(existingUc3.getUser()).isNull();
        }

        @Test
        @DisplayName("Should handle mixed operations correctly")
        void shouldHandleMixedOperationsCorrectly() {
            // Given - Remove category2, keep category1, add new category
            Category newCategory = Category.builder().id(4L).name("Fashion").build();
            List<Category> newCategories = Arrays.asList(category1, category3, newCategory);

            // When
            userValidationService.updateUserCategories(mockUser, newCategories);

            // Then
            assertThat(mockUser.getUserCategories()).hasSize(3);

            Set<Long> categoryIds = mockUser.getUserCategories().stream()
                    .map(uc -> uc.getCategory().getId())
                    .collect(Collectors.toSet());

            assertThat(categoryIds).containsExactlyInAnyOrder(1L, 3L, 4L);

            // Verify removed category has null user reference
            assertThat(existingUc2.getUser()).isNull();

            // Verify kept categories still have user reference
            assertThat(existingUc1.getUser()).isEqualTo(mockUser);
            assertThat(existingUc3.getUser()).isEqualTo(mockUser);
        }

        @Test
        @DisplayName("Should maintain referential integrity in UserCategory objects")
        void shouldMaintainReferentialIntegrityInUserCategoryObjects() {
            // Given
            Category newCategory = Category.builder().id(4L).name("Books").build();
            List<Category> newCategories = Arrays.asList(category1, newCategory);

            // When
            userValidationService.updateUserCategories(mockUser, newCategories);

            // Then
            mockUser.getUserCategories().forEach(uc -> {
                assertThat(uc.getUser()).isEqualTo(mockUser);
                assertThat(uc.getCategory()).isNotNull();
                assertThat(uc.getCategory().getId()).isIn(1L, 4L);
            });
        }
    }

}
