package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CategoryDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCategoryDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.CategoryMapper;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryDTO> listCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toCategoryDTO)
                .toList();
    }

    public CategoryDTO createCategory(@NotNull CreateCategoryDTO dto) {
        if (Boolean.TRUE.equals(categoryRepository.existsByName(dto.getName()))) {
            throw new MessageException("Category with name '" + dto.getName() + "' already exists");
        }

        Category newCategory = categoryMapper.toEntity(dto);

        return categoryMapper.toCategoryDTO(categoryRepository.save(newCategory));
    }

    public CategoryDTO updateCategory(Long id, @NotNull CreateCategoryDTO dto) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new MessageException("Category not found with id: " + id));

        if (dto.getName() != null &&
                !existingCategory.getName().equals(dto.getName()) &&
                Boolean.TRUE.equals(categoryRepository.existsByName(dto.getName()))) {

            throw new MessageException("Another category already has the name: " + dto.getName());
        }


        categoryMapper.updateFromDto(dto, existingCategory);

        return categoryMapper.toCategoryDTO(categoryRepository.save(existingCategory));
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new MessageException("Category not found with id: " + id);
        }

        if (isCategoryInUse(id)) {
            throw new MessageException("Cannot delete category because it's in use");
        }

        categoryRepository.deleteById(id);
    }

    private boolean isCategoryInUse(Long categoryId) {
        return categoryRepository.isCategoryInUse(categoryId);
    }

}
