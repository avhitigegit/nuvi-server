package com.nuvi.online_renting.categories.service;

import com.nuvi.online_renting.categories.dto.CategoryRequestDTO;
import com.nuvi.online_renting.categories.dto.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {

    CategoryResponseDTO createCategory(CategoryRequestDTO dto);

    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO dto);

    void deleteCategory(Long id);

    CategoryResponseDTO getCategoryById(Long id);

    List<CategoryResponseDTO> getAllCategories();
}
