package com.nuvi.online_renting.item.controller;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.service.ItemService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ITEM')")
    public ResponseEntity<ItemResponseDTO> createItem(@RequestBody ItemRequestDTO itemRequestDTO) {
        return ResponseEntity.ok(itemService.createItem(itemRequestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    // Search + Pagination
    // GET /api/items?name=bike&minPrice=500&maxPrice=3000&available=true&sellerId=2&page=0&size=10&sort=pricePerDay,asc
    @GetMapping
    public ResponseEntity<PagedResponse<ItemResponseDTO>> getAllItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Long sellerId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(itemService.searchItems(name, minPrice, maxPrice, available, sellerId, pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CREATE_ITEM')")
    public ResponseEntity<PagedResponse<ItemResponseDTO>> getMyItems(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(itemService.getMyItems(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<ItemResponseDTO> updateItem(@PathVariable Long id,
                                                      @RequestBody ItemRequestDTO itemRequestDTO) {
        return ResponseEntity.ok(itemService.updateItem(id, itemRequestDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DELETE_OWN_ITEM', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}