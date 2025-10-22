package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CategoryDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCategoryDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.CategoryMapper;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private static final Long TEST_CATEGORY_ID = 1L;
    private static final String TEST_CATEGORY_NAME = "Technology";
    private static final String DUPLICATE_NAME = "Duplicate";
    private static final String CATEGORY_NOT_FOUND_PREFIX = "Category not found with id: ";
    private static final String CATEGORY_IN_USE_MESSAGE = "Cannot delete category because it's in use";

    private Category testCategory;
    private CreateCategoryDTO createCategoryDTO;
    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(TEST_CATEGORY_ID);
        testCategory.setName(TEST_CATEGORY_NAME);
        testCategory.setDescription("Test Description");

        createCategoryDTO = new CreateCategoryDTO();
        createCategoryDTO.setName(TEST_CATEGORY_NAME);
        createCategoryDTO.setDescription("Test Description");

        categoryDTO = new CategoryDTO();
        categoryDTO.setId(TEST_CATEGORY_ID);
        categoryDTO.setName(TEST_CATEGORY_NAME);
        categoryDTO.setDescription("Test Description");
    }

    @Nested
    @DisplayName("listCategories() tests")
    class ListCategoriesTests {

        @Test
        @DisplayName("Should return list of mapped category DTOs when categories exist")
        void listCategories_WithCategories_ShouldReturnMappedDTOs() {
            // Given
            Category category1 = createCategory(1L, "Category 1");
            Category category2 = createCategory(2L, "Category 2");
            Category category3 = createCategory(3L, "Category 3");
            List<Category> categories = List.of(category1, category2, category3);

            CategoryDTO dto1 = createCategoryDTO(1L, "Category 1");
            CategoryDTO dto2 = createCategoryDTO(2L, "Category 2");
            CategoryDTO dto3 = createCategoryDTO(3L, "Category 3");

            when(categoryRepository.findAll()).thenReturn(categories);
            when(categoryMapper.toCategoryDTO(category1)).thenReturn(dto1);
            when(categoryMapper.toCategoryDTO(category2)).thenReturn(dto2);
            when(categoryMapper.toCategoryDTO(category3)).thenReturn(dto3);

            // When
            List<CategoryDTO> result = categoryService.listCategories();

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasSize(3)
                    .containsExactly(dto1, dto2, dto3);

            verify(categoryRepository).findAll();
            verify(categoryMapper).toCategoryDTO(category1);
            verify(categoryMapper).toCategoryDTO(category2);
            verify(categoryMapper).toCategoryDTO(category3);
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void listCategories_WithEmptyList_ShouldReturnEmptyList() {
            // Given
            when(categoryRepository.findAll()).thenReturn(List.of());

            // When
            List<CategoryDTO> result = categoryService.listCategories();

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEmpty();

            verify(categoryRepository).findAll();
        }

        @Test
        @DisplayName("Should use mapper for each category")
        void listCategories_ShouldUseMapper() {
            // Given
            Category category = createCategory(1L, "Category");
            when(categoryRepository.findAll()).thenReturn(List.of(category));
            when(categoryMapper.toCategoryDTO(category)).thenReturn(categoryDTO);

            // When
            categoryService.listCategories();

            // Then
            verify(categoryMapper).toCategoryDTO(category);
        }

        @Test
        @DisplayName("Should stream all categories from repository")
        void listCategories_ShouldStreamAllCategories() {
            // Given
            List<Category> categories = List.of(
                    createCategory(1L, "Cat1"),
                    createCategory(2L, "Cat2")
            );
            when(categoryRepository.findAll()).thenReturn(categories);
            when(categoryMapper.toCategoryDTO(any())).thenReturn(categoryDTO);

            // When
            List<CategoryDTO> result = categoryService.listCategories();

            // Then
            assertThat(result).hasSize(2);
            verify(categoryMapper, times(2)).toCategoryDTO(any());
        }
    }

    @Nested
    @DisplayName("createCategory() tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category successfully with valid data")
        void createCategory_WithValidDTO_ShouldCreateCategory() {
            // Given
            when(categoryRepository.existsByName(TEST_CATEGORY_NAME)).thenReturn(Boolean.FALSE);
            when(categoryMapper.toEntity(createCategoryDTO)).thenReturn(testCategory);
            when(categoryRepository.save(testCategory)).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(categoryDTO);

            // When
            CategoryDTO result = categoryService.createCategory(createCategoryDTO);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(categoryDTO);

            verify(categoryRepository).existsByName(TEST_CATEGORY_NAME);
            verify(categoryMapper).toEntity(createCategoryDTO);
            verify(categoryRepository).save(testCategory);
            verify(categoryMapper).toCategoryDTO(testCategory);
        }

        @Test
        @DisplayName("Should throw exception when category name already exists")
        void createCategory_WithDuplicateName_ShouldThrowException() {
            // Given
            when(categoryRepository.existsByName(TEST_CATEGORY_NAME)).thenReturn(Boolean.TRUE);

            // When & Then
            assertThatThrownBy(() -> categoryService.createCategory(createCategoryDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Category with name '" + TEST_CATEGORY_NAME + "' already exists");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should check name existence before creating")
        void createCategory_ShouldCheckNameExistence() {
            // Given
            when(categoryRepository.existsByName(TEST_CATEGORY_NAME)).thenReturn(Boolean.FALSE);
            when(categoryMapper.toEntity(any())).thenReturn(testCategory);
            when(categoryRepository.save(any())).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(any())).thenReturn(categoryDTO);

            // When
            categoryService.createCategory(createCategoryDTO);

            // Then
            verify(categoryRepository).existsByName(TEST_CATEGORY_NAME);
        }

        @Test
        @DisplayName("Should handle Boolean.TRUE correctly for duplicate check")
        void createCategory_ShouldHandleBooleanTrueCorrectly() {
            // Given
            when(categoryRepository.existsByName(TEST_CATEGORY_NAME)).thenReturn(Boolean.TRUE);

            // When & Then
            assertThatThrownBy(() -> categoryService.createCategory(createCategoryDTO))
                    .isInstanceOf(MessageException.class);

            verify(categoryRepository).existsByName(TEST_CATEGORY_NAME);
        }

        @Test
        @DisplayName("Should proceed with creation when existsByName returns FALSE")
        void createCategory_WithExistsByNameFalse_ShouldProceed() {
            // Given
            when(categoryRepository.existsByName(TEST_CATEGORY_NAME)).thenReturn(Boolean.FALSE);
            when(categoryMapper.toEntity(createCategoryDTO)).thenReturn(testCategory);
            when(categoryRepository.save(testCategory)).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(categoryDTO);

            // When
            CategoryDTO result = categoryService.createCategory(createCategoryDTO);

            // Then
            assertThat(result).isNotNull();
            verify(categoryRepository).save(testCategory);
        }

        @Test
        @DisplayName("Should map DTO to entity before saving")
        void createCategory_ShouldMapDTOToEntity() {
            // Given
            when(categoryRepository.existsByName(TEST_CATEGORY_NAME)).thenReturn(Boolean.FALSE);
            when(categoryMapper.toEntity(createCategoryDTO)).thenReturn(testCategory);
            when(categoryRepository.save(any())).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(any())).thenReturn(categoryDTO);

            // When
            categoryService.createCategory(createCategoryDTO);

            // Then
            verify(categoryMapper).toEntity(createCategoryDTO);
        }
    }

    @Nested
    @DisplayName("updateCategory() tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update category successfully with valid data")
        void updateCategory_WithValidData_ShouldUpdateCategory() {
            // Given
            CreateCategoryDTO updateDTO = new CreateCategoryDTO();
            updateDTO.setName("Updated Name");
            updateDTO.setDescription("Updated Description");

            when(categoryRepository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByName("Updated Name")).thenReturn(Boolean.FALSE);
            when(categoryRepository.save(testCategory)).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(categoryDTO);

            // When
            CategoryDTO result = categoryService.updateCategory(TEST_CATEGORY_ID, updateDTO);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(categoryDTO);

            verify(categoryRepository).findById(TEST_CATEGORY_ID);
            verify(categoryMapper).updateFromDto(updateDTO, testCategory);
            verify(categoryRepository).save(testCategory);
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void updateCategory_WithNonExistentCategory_ShouldThrowException() {
            // Given
            when(categoryRepository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategory(TEST_CATEGORY_ID, createCategoryDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(CATEGORY_NOT_FOUND_PREFIX + TEST_CATEGORY_ID);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when new name already exists on different category")
        void updateCategory_WithDuplicateName_ShouldThrowException() {
            // Given
            CreateCategoryDTO updateDTO = new CreateCategoryDTO();
            updateDTO.setName(DUPLICATE_NAME);

            when(categoryRepository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByName(DUPLICATE_NAME)).thenReturn(Boolean.TRUE);

            // When & Then
            assertThatThrownBy(() -> categoryService.updateCategory(TEST_CATEGORY_ID, updateDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Another category already has the name: " + DUPLICATE_NAME);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow update when name is same as existing category name")
        void updateCategory_WithSameName_ShouldAllowUpdate() {
            // Given
            CreateCategoryDTO updateDTO = new CreateCategoryDTO();
            updateDTO.setName(TEST_CATEGORY_NAME); // Same name as existing
            updateDTO.setDescription("New Description");

            when(categoryRepository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(testCategory)).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(categoryDTO);

            // When
            CategoryDTO result = categoryService.updateCategory(TEST_CATEGORY_ID, updateDTO);

            // Then
            assertThat(result).isNotNull();
            verify(categoryRepository).save(testCategory);
            verify(categoryRepository, never()).existsByName(any());
        }

        @Test
        @DisplayName("Should skip name validation when DTO name is null")
        void updateCategory_WithNullName_ShouldSkipNameValidation() {
            // Given
            CreateCategoryDTO updateDTO = new CreateCategoryDTO();
            updateDTO.setName(null);
            updateDTO.setDescription("Only updating description");

            when(categoryRepository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(testCategory)).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(categoryDTO);

            // When
            categoryService.updateCategory(TEST_CATEGORY_ID, updateDTO);

            // Then
            verify(categoryRepository, never()).existsByName(any());
            verify(categoryRepository).save(testCategory);
        }

        @Test
        @DisplayName("Should check name uniqueness only when name is different")
        void updateCategory_ShouldCheckNameUniqueness() {
            // Given
            CreateCategoryDTO updateDTO = new CreateCategoryDTO();
            updateDTO.setName("Different Name");

            when(categoryRepository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByName("Different Name")).thenReturn(Boolean.FALSE);
            when(categoryRepository.save(any())).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(any())).thenReturn(categoryDTO);

            // When
            categoryService.updateCategory(TEST_CATEGORY_ID, updateDTO);

            // Then
            verify(categoryRepository).existsByName("Different Name");
        }

        @Test
        @DisplayName("Should use mapper to update from DTO")
        void updateCategory_ShouldUpdateFromDTO() {
            // Given
            when(categoryRepository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(any())).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(any())).thenReturn(categoryDTO);

            // When
            categoryService.updateCategory(TEST_CATEGORY_ID, createCategoryDTO);

            // Then
            verify(categoryMapper).updateFromDto(createCategoryDTO, testCategory);
        }

        @Test
        @DisplayName("Should not check duplicate when same name as existing")
        void updateCategory_WithSameNameAsExisting_ShouldNotCheckDuplicate() {
            // Given
            CreateCategoryDTO updateDTO = new CreateCategoryDTO();
            updateDTO.setName(TEST_CATEGORY_NAME);

            when(categoryRepository.findById(TEST_CATEGORY_ID)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(any())).thenReturn(testCategory);
            when(categoryMapper.toCategoryDTO(any())).thenReturn(categoryDTO);

            // When
            categoryService.updateCategory(TEST_CATEGORY_ID, updateDTO);

            // Then
            verify(categoryRepository, never()).existsByName(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory() tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category successfully when category exists and not in use")
        void deleteCategory_WithValidId_ShouldDeleteCategory() {
            // Given
            when(categoryRepository.existsById(TEST_CATEGORY_ID)).thenReturn(true);
            when(categoryRepository.isCategoryInUse(TEST_CATEGORY_ID)).thenReturn(false);

            // When
            categoryService.deleteCategory(TEST_CATEGORY_ID);

            // Then
            verify(categoryRepository).existsById(TEST_CATEGORY_ID);
            verify(categoryRepository).isCategoryInUse(TEST_CATEGORY_ID);
            verify(categoryRepository).deleteById(TEST_CATEGORY_ID);
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void deleteCategory_WithNonExistentCategory_ShouldThrowException() {
            // Given
            when(categoryRepository.existsById(TEST_CATEGORY_ID)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> categoryService.deleteCategory(TEST_CATEGORY_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(CATEGORY_NOT_FOUND_PREFIX + TEST_CATEGORY_ID);

            verify(categoryRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when category is in use")
        void deleteCategory_WithCategoryInUse_ShouldThrowException() {
            // Given
            when(categoryRepository.existsById(TEST_CATEGORY_ID)).thenReturn(true);
            when(categoryRepository.isCategoryInUse(TEST_CATEGORY_ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> categoryService.deleteCategory(TEST_CATEGORY_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(CATEGORY_IN_USE_MESSAGE);

            verify(categoryRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should check existence before checking usage")
        void deleteCategory_ShouldCheckExistenceFirst() {
            // Given
            when(categoryRepository.existsById(TEST_CATEGORY_ID)).thenReturn(true);
            when(categoryRepository.isCategoryInUse(TEST_CATEGORY_ID)).thenReturn(false);

            // When
            categoryService.deleteCategory(TEST_CATEGORY_ID);

            // Then
            verify(categoryRepository).existsById(TEST_CATEGORY_ID);
        }

        @Test
        @DisplayName("Should check usage before deleting")
        void deleteCategory_ShouldCheckUsageBeforeDeleting() {
            // Given
            when(categoryRepository.existsById(TEST_CATEGORY_ID)).thenReturn(true);
            when(categoryRepository.isCategoryInUse(TEST_CATEGORY_ID)).thenReturn(false);

            // When
            categoryService.deleteCategory(TEST_CATEGORY_ID);

            // Then
            verify(categoryRepository).isCategoryInUse(TEST_CATEGORY_ID);
        }

        @Test
        @DisplayName("Should not delete when category is in use")
        void deleteCategory_WithInUseCategory_ShouldNotDelete() {
            // Given
            when(categoryRepository.existsById(TEST_CATEGORY_ID)).thenReturn(true);
            when(categoryRepository.isCategoryInUse(TEST_CATEGORY_ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> categoryService.deleteCategory(TEST_CATEGORY_ID))
                    .isInstanceOf(MessageException.class);

            verify(categoryRepository, never()).deleteById(TEST_CATEGORY_ID);
        }

        @Test
        @DisplayName("Should call isCategoryInUse through repository")
        void deleteCategory_ShouldCallIsCategoryInUse() {
            // Given
            when(categoryRepository.existsById(TEST_CATEGORY_ID)).thenReturn(true);
            when(categoryRepository.isCategoryInUse(TEST_CATEGORY_ID)).thenReturn(false);

            // When
            categoryService.deleteCategory(TEST_CATEGORY_ID);

            // Then
            verify(categoryRepository).isCategoryInUse(TEST_CATEGORY_ID);
        }
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription("Description for " + name);
        return category;
    }

    private CategoryDTO createCategoryDTO(Long id, String name) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setDescription("Description for " + name);
        return dto;
    }
}
