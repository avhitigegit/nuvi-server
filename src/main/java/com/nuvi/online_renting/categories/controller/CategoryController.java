package com.nuvi.online_renting.categories.controller;

import com.nuvi.online_renting.categories.dto.CategoryRequestDTO;
import com.nuvi.online_renting.categories.dto.CategoryResponseDTO;
import com.nuvi.online_renting.categories.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Categories", description = "Item category management. Admins manage categories; all users can browse them.")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Create a category", description = "Admin only. Creates a new item category.")
    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasAuthority('MANAGE_CATEGORIES')")
    public ResponseEntity<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(dto));
    }

    @Operation(summary = "Update a category", description = "Admin only. Updates an existing category.")
    @PutMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CATEGORIES')")
    public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable Long id,
                                                               @Valid @RequestBody CategoryRequestDTO dto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, dto));
    }

    @Operation(summary = "Delete a category", description = "Admin only. Deletes a category. Items in this category will have their category set to null.")
    @DeleteMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CATEGORIES')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all categories", description = "Public. Returns all available item categories.")
    @GetMapping("/api/v1/categories")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @Operation(summary = "Get category by ID", description = "Public. Returns a single category by its ID.")
    @GetMapping("/api/v1/categories/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }
}
