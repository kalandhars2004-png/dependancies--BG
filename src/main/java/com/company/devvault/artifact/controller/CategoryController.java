package com.company.devvault.artifact.controller;

import com.company.devvault.artifact.entity.Category;
import com.company.devvault.artifact.repository.CategoryRepository;
import com.company.devvault.common.exception.ApiException;
import com.company.devvault.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ApiResponse<List<Category>> list() {
        return ApiResponse.success(categoryRepository.findAllByOrderByNameAsc());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Category> create(@RequestBody CreateCategoryRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw ApiException.badRequest("Category name is required");
        }
        String name = request.getName().trim();
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw ApiException.conflict("Category already exists: " + name);
        }
        Category category = new Category();
        category.setName(name);
        category.setSlug(name.toLowerCase(Locale.ROOT).replace(' ', '-'));
        category.setDescription(request.getDescription());
        return ApiResponse.success("Category created", categoryRepository.save(category));
    }

    public static class CreateCategoryRequest {
        private String name;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}