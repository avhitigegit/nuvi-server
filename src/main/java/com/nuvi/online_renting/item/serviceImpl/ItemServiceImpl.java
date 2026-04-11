package com.nuvi.online_renting.item.serviceImpl;

import com.nuvi.online_renting.categories.model.Category;
import com.nuvi.online_renting.categories.repository.CategoryRepository;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.item.dto.ItemBlockedDateRequestDTO;
import com.nuvi.online_renting.item.dto.ItemBlockedDateResponseDTO;
import com.nuvi.online_renting.item.dto.ItemImageResponseDTO;
import com.nuvi.online_renting.item.model.ItemBlockedDate;
import com.nuvi.online_renting.item.model.ItemImage;
import com.nuvi.online_renting.item.repository.ItemBlockedDateRepository;
import com.nuvi.online_renting.item.repository.ItemImageRepository;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.common.storage.S3StorageService;
import com.nuvi.online_renting.common.validation.FileValidator;
import com.nuvi.online_renting.item.dto.ItemRequestDTO;
import com.nuvi.online_renting.item.dto.ItemResponseDTO;
import com.nuvi.online_renting.item.model.Item;
import com.nuvi.online_renting.item.repository.ItemRepository;
import com.nuvi.online_renting.item.service.ItemService;
import com.nuvi.online_renting.currency.service.ExchangeRateService;
import com.nuvi.online_renting.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ItemServiceImpl implements ItemService {

    private static final int MAX_GALLERY_IMAGES = 10;

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final ItemBlockedDateRepository blockedDateRepository;
    private final ItemImageRepository itemImageRepository;
    private final AuthenticationFacade authFacade;
    private final S3StorageService s3StorageService;
    private final FileValidator fileValidator;
    private final ExchangeRateService exchangeRateService;

    public ItemServiceImpl(ItemRepository itemRepository,
                           CategoryRepository categoryRepository,
                           ItemBlockedDateRepository blockedDateRepository,
                           ItemImageRepository itemImageRepository,
                           AuthenticationFacade authFacade,
                           S3StorageService s3StorageService,
                           FileValidator fileValidator,
                           ExchangeRateService exchangeRateService) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.blockedDateRepository = blockedDateRepository;
        this.itemImageRepository = itemImageRepository;
        this.authFacade = authFacade;
        this.s3StorageService = s3StorageService;
        this.fileValidator = fileValidator;
        this.exchangeRateService = exchangeRateService;
    }

    @Override
    @Transactional
    public ItemResponseDTO createItem(ItemRequestDTO dto) {
        User currentUser = authFacade.getCurrentUser();

        if (!currentUser.isKycVerified()) {
            throw new ForbiddenException("Your identity (KYC) has not been verified yet. " +
                    "Please complete your seller application and wait for admin approval before listing items.");
        }

        if (currentUser.isSuspended()) {
            throw new ForbiddenException("Your seller account has been suspended. " +
                    "You cannot list new items. Please contact support.");
        }

        Item item = new Item();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPricePerDay(dto.getPricePerDay());
        item.setAvailable(dto.getAvailable() != null ? dto.getAvailable() : true);
        item.setSeller(currentUser);
        item.setLatitude(dto.getLatitude());
        item.setLongitude(dto.getLongitude());
        item.setCategory(resolveCategory(dto.getCategoryId()));

        return convertToResponseDTO(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemResponseDTO getItemById(Long id, String currency) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));
        return convertToResponseDTO(item, currency);
    }

    @Override
    @Transactional
    public PagedResponse<ItemResponseDTO> searchItems(String name, Double minPrice, Double maxPrice,
                                                      Boolean available, Long sellerId, Long categoryId,
                                                      Double lat, Double lng, Double radiusKm,
                                                      String currency, Pageable pageable) {
        // Location search: fetch candidates with coordinates, filter by Haversine in Java
        if (lat != null && lng != null && radiusKm != null) {
            List<Item> candidates = itemRepository.findItemsWithCoordinates(name, minPrice, maxPrice, available, sellerId, categoryId);

            List<ItemResponseDTO> filtered = new ArrayList<>();
            for (Item item : candidates) {
                double distance = haversineKm(lat, lng, item.getLatitude(), item.getLongitude());
                if (distance <= radiusKm) {
                    ItemResponseDTO dto = convertToResponseDTO(item, currency);
                    dto.setDistanceKm(Math.round(distance * 10.0) / 10.0);
                    filtered.add(dto);
                }
            }

            // Sort by distance ascending
            filtered.sort(Comparator.comparingDouble(ItemResponseDTO::getDistanceKm));

            // Manual pagination
            int pageNum = pageable.getPageNumber();
            int pageSize = pageable.getPageSize();
            int start = Math.min(pageNum * pageSize, filtered.size());
            int end = Math.min(start + pageSize, filtered.size());
            List<ItemResponseDTO> pageContent = filtered.subList(start, end);

            return new PagedResponse<>(pageContent, pageNum, pageSize, filtered.size());
        }

        // Standard search without location
        Page<Item> page = itemRepository.searchItems(name, minPrice, maxPrice, available, sellerId, categoryId, pageable);
        return new PagedResponse<>(page.map(item -> convertToResponseDTO(item, currency)));
    }

    /**
     * Haversine formula — calculates great-circle distance between two GPS points in km.
     */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    @Transactional
    public ItemResponseDTO updateItem(Long id, ItemRequestDTO dto) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to update this item");
        }

        if (!isAdmin && !currentUser.isKycVerified()) {
            throw new ForbiddenException("Your identity (KYC) has not been verified. You cannot update items until your seller application is approved.");
        }

        if (!isAdmin && currentUser.isSuspended()) {
            throw new ForbiddenException("Your seller account has been suspended. You cannot update items. Please contact support.");
        }

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPricePerDay(dto.getPricePerDay());
        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }
        item.setLatitude(dto.getLatitude());
        item.setLongitude(dto.getLongitude());
        item.setCategory(resolveCategory(dto.getCategoryId()));

        return convertToResponseDTO(itemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to delete this item");
        }

        item.setDeleted(true);
        item.setDeletedAt(LocalDateTime.now());
        itemRepository.save(item);
    }

    @Override
    @Transactional
    public PagedResponse<ItemResponseDTO> getMyItems(Pageable pageable) {
        Long sellerId = authFacade.getCurrentUser().getId();
        Page<Item> page = itemRepository.findBySellerId(sellerId, pageable);
        return new PagedResponse<>(page.map(item -> convertToResponseDTO(item, null)));
    }

    @Override
    @Transactional
    public ItemResponseDTO uploadImage(Long id, MultipartFile file) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to upload an image for this item");
        }

        // Centralised MIME validation — extension whitelist + magic-byte content check
        fileValidator.validateImage(file);

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        String s3Key = "items/item_" + id + "_" + System.currentTimeMillis() + "." + ext;
        s3StorageService.uploadFile(s3Key, file);

        // Store the S3 key — pre-signed URL is generated on each GET request
        item.setImageUrl(s3Key);
        return convertToResponseDTO(itemRepository.save(item));
    }

    @Override
    public String getImagePath(Long id) {
        String key = itemRepository.findById(id)
                .map(Item::getImageUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));
        return s3StorageService.generateItemImageUrl(key);
    }

    @Override
    @Transactional
    public ItemBlockedDateResponseDTO addBlockedDate(Long itemId, ItemBlockedDateRequestDTO dto) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + itemId));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You can only manage availability for your own items");
        }

        if (!dto.getEndDate().isAfter(dto.getStartDate()) && !dto.getEndDate().isEqual(dto.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        if (blockedDateRepository.existsOverlappingBlock(itemId, dto.getStartDate(), dto.getEndDate())) {
            throw new BadRequestException("The selected date range overlaps with an existing blocked period");
        }

        ItemBlockedDate block = new ItemBlockedDate();
        block.setItem(item);
        block.setStartDate(dto.getStartDate());
        block.setEndDate(dto.getEndDate());
        block.setReason(dto.getReason());

        return toBlockedDateDTO(blockedDateRepository.save(block));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemBlockedDateResponseDTO> getBlockedDates(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id " + itemId);
        }
        return blockedDateRepository.findByItemIdOrderByStartDateAsc(itemId)
                .stream().map(this::toBlockedDateDTO).toList();
    }

    @Override
    @Transactional
    public void removeBlockedDate(Long itemId, Long blockedDateId) {
        ItemBlockedDate block = blockedDateRepository.findById(blockedDateId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked date not found with id " + blockedDateId));

        if (!block.getItem().getId().equals(itemId)) {
            throw new BadRequestException("Blocked date does not belong to item " + itemId);
        }

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = block.getItem().getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You can only manage availability for your own items");
        }

        blockedDateRepository.delete(block);
    }

    @Override
    @Transactional
    public ItemImageResponseDTO addGalleryImage(Long itemId, MultipartFile file, int displayOrder) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + itemId));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = item.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to upload images for this item");
        }

        if (itemImageRepository.countByItemId(itemId) >= MAX_GALLERY_IMAGES) {
            throw new BadRequestException("Maximum of " + MAX_GALLERY_IMAGES + " gallery images per item reached");
        }

        fileValidator.validateImage(file);

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        String s3Key = "items/gallery_" + itemId + "_" + System.currentTimeMillis() + "." + ext;
        s3StorageService.uploadFile(s3Key, file);

        ItemImage image = new ItemImage();
        image.setItem(item);
        image.setS3Key(s3Key);
        image.setDisplayOrder(displayOrder);

        return toImageDTO(itemImageRepository.save(image));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemImageResponseDTO> getGalleryImages(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id " + itemId);
        }
        return itemImageRepository.findByItemIdOrderByDisplayOrderAscCreatedAtAsc(itemId)
                .stream().map(this::toImageDTO).toList();
    }

    @Override
    @Transactional
    public void deleteGalleryImage(Long itemId, Long imageId) {
        ItemImage image = itemImageRepository.findByIdAndItemId(imageId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image " + imageId + " not found for item " + itemId));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = image.getItem().getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to delete images for this item");
        }

        s3StorageService.deleteFile(image.getS3Key());
        itemImageRepository.delete(image);
    }

    private ItemImageResponseDTO toImageDTO(ItemImage image) {
        ItemImageResponseDTO dto = new ItemImageResponseDTO();
        dto.setId(image.getId());
        dto.setItemId(image.getItem().getId());
        dto.setImageUrl(s3StorageService.generateItemImageUrl(image.getS3Key()));
        dto.setDisplayOrder(image.getDisplayOrder());
        dto.setCreatedAt(image.getCreatedAt());
        dto.setCreatedBy(image.getCreatedBy());
        return dto;
    }

    private ItemBlockedDateResponseDTO toBlockedDateDTO(ItemBlockedDate block) {
        ItemBlockedDateResponseDTO dto = new ItemBlockedDateResponseDTO();
        dto.setId(block.getId());
        dto.setItemId(block.getItem().getId());
        dto.setStartDate(block.getStartDate());
        dto.setEndDate(block.getEndDate());
        dto.setReason(block.getReason());
        dto.setCreatedAt(block.getCreatedAt());
        dto.setCreatedBy(block.getCreatedBy());
        return dto;
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + categoryId));
    }

    private ItemResponseDTO convertToResponseDTO(Item item) {
        return convertToResponseDTO(item, null);
    }

    private ItemResponseDTO convertToResponseDTO(Item item, String currency) {
        ItemResponseDTO dto = new ItemResponseDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setPricePerDay(item.getPricePerDay());
        dto.setAvailable(item.isAvailable());
        dto.setSellerId(item.getSeller().getId());
        dto.setSellerName(item.getSeller().getName());
        // imageUrl in DB is an S3 key — generate a short-lived pre-signed URL for the response
        dto.setImageUrl(s3StorageService.generateItemImageUrl(item.getImageUrl()));
        dto.setLatitude(item.getLatitude());
        dto.setLongitude(item.getLongitude());
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
        }
        dto.setAverageRating(item.getAverageRating());
        dto.setTotalReviews(item.getTotalReviews());
        // Currency conversion — display only, base currency is always LKR
        if (currency != null && !currency.isBlank() && !currency.equalsIgnoreCase("LKR")) {
            dto.setDisplayPrice(exchangeRateService.convert(item.getPricePerDay(), currency));
            dto.setDisplayCurrency(currency.toUpperCase());
        }
        // Gallery images — pre-signed URLs ordered by displayOrder
        List<String> galleryUrls = itemImageRepository
                .findByItemIdOrderByDisplayOrderAscCreatedAtAsc(item.getId())
                .stream()
                .map(img -> s3StorageService.generateItemImageUrl(img.getS3Key()))
                .toList();
        dto.setImages(galleryUrls);
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        dto.setCreatedBy(item.getCreatedBy());
        dto.setUpdatedBy(item.getUpdatedBy());
        return dto;
    }
}
