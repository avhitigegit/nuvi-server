package com.nuvi.online_renting.item.controller;

import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','SELLER')")
    public ResponseEntity<ItemResponseDTO> createItem(@RequestBody ItemRequestDTO itemRequestDTO) {
        return ResponseEntity.ok(itemService.createItem(itemRequestDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','SELLER', 'ADMIN')")
    public ResponseEntity<ItemResponseDTO> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','SELLER', 'ADMIN')")
    public ResponseEntity<List<ItemResponseDTO>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','SELLER', 'ADMIN')")
    public ResponseEntity<ItemResponseDTO> updateItem(@PathVariable Long id, @RequestBody ItemRequestDTO itemRequestDTO) {
        return ResponseEntity.ok(itemService.updateItem(id, itemRequestDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','SELLER', 'ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
