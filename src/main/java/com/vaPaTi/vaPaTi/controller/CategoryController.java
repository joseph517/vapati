package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.CategoryDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCategoryDTO;
import com.vaPaTi.vaPaTi.entity.Category;
import com.vaPaTi.vaPaTi.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "Category API")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "Get all categories", description = "Get all categories")
    public ResponseEntity<List<CategoryDTO>> listCategories() {
        List<CategoryDTO> categories = categoryService.listCategories();
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/create")
    @Operation(summary = "Create new category", description = "Create a new category")
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CreateCategoryDTO dto) {
        CategoryDTO newCategory = categoryService.createCategory(dto);
        return ResponseEntity.ok(newCategory);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update category", description = "Update category by ID")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long id, @RequestBody CreateCategoryDTO dto) {
        CategoryDTO updatedCategory = categoryService.updateCategory(id, dto);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete category", description = "Delete category by ID")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Deleted");
    }
}
