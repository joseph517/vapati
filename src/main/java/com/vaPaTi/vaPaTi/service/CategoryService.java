package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CategoryDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCategoryDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.CategoryMapper;
import com.vaPaTi.vaPaTi.repository.CampaignCategoryRepository;
import com.vaPaTi.vaPaTi.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CampaignCategoryRepository campaignCategoryRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (dto.getName() != null &&
                !existingCategory.getName().equals(dto.getName()) &&
                Boolean.TRUE.equals(categoryRepository.existsByName(dto.getName()))) {

            throw new MessageException("Another category already has the name: " + dto.getName());
        }


        categoryMapper.updateFromDto(dto, existingCategory);

        return categoryMapper.toCategoryDTO(categoryRepository.save(existingCategory));
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }

        if (isCategoryInUse(id)) {
            throw new MessageException("Cannot delete category because it's in use");
        }

        if (!categoryRepository.findCampaignIdsThatWouldBeOrphaned(id).isEmpty()) {
            throw new MessageException("Cannot delete category because it would leave campaigns without any category");
        }

        campaignCategoryRepository.deleteAll(campaignCategoryRepository.findByCategoryId(id));

        categoryRepository.deleteById(id);
    }

    private boolean isCategoryInUse(Long categoryId) {
        return categoryRepository.isCategoryInUse(categoryId);
    }

}
