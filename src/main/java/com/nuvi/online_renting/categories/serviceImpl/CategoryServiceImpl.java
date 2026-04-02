package com.nuvi.online_renting.categories.serviceImpl;

import com.nuvi.online_renting.categories.dto.CategoryRequestDTO;
import com.nuvi.online_renting.categories.dto.CategoryResponseDTO;
import com.nuvi.online_renting.categories.model.Category;
import com.nuvi.online_renting.categories.repository.CategoryRepository;
import com.nuvi.online_renting.categories.service.CategoryService;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new BadRequestException("A category with the name '" + dto.getName() + "' already exists.");
        }

        Category category = new Category();
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());

        return toDTO(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO dto) {
        Category category = findOrThrow(id);

        // Allow same name on update (only block if another category has the same name)
        categoryRepository.findByNameIgnoreCase(dto.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("A category with the name '" + dto.getName() + "' already exists.");
                });

        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());

        return toDTO(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findOrThrow(id);
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
    }

    private CategoryResponseDTO toDTO(Category category) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        dto.setCreatedBy(category.getCreatedBy());
        dto.setUpdatedBy(category.getUpdatedBy());
        return dto;
    }
}
