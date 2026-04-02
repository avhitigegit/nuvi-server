package com.nuvi.online_renting.coupons.serviceImpl;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.DiscountType;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ConflictException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.coupons.dto.CouponRequestDTO;
import com.nuvi.online_renting.coupons.dto.CouponResponseDTO;
import com.nuvi.online_renting.coupons.dto.ValidateCouponRequestDTO;
import com.nuvi.online_renting.coupons.dto.ValidateCouponResponseDTO;
import com.nuvi.online_renting.coupons.model.Coupon;
import com.nuvi.online_renting.coupons.repository.CouponRepository;
import com.nuvi.online_renting.coupons.service.CouponService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional
    public CouponResponseDTO createCoupon(CouponRequestDTO request) {
        String code = request.getCode().toUpperCase();

        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("A coupon with code '" + code + "' already exists");
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE && request.getDiscountValue() > 100) {
            throw new BadRequestException("Percentage discount cannot exceed 100%");
        }

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinBookingAmount(request.getMinBookingAmount());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setActive(true);

        return toDTO(couponRepository.save(coupon));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CouponResponseDTO> getAllCoupons(Pageable pageable) {
        return new PagedResponse<>(couponRepository.findAll(pageable).map(this::toDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDTO getCoupon(Long id) {
        return toDTO(getOrThrow(id));
    }

    @Override
    @Transactional
    public CouponResponseDTO deactivateCoupon(Long id) {
        Coupon coupon = getOrThrow(id);
        coupon.setActive(false);
        return toDTO(couponRepository.save(coupon));
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateCouponResponseDTO validateCoupon(ValidateCouponRequestDTO request) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(request.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon code '" + request.getCode() + "' is not valid"));

        validateCouponApplicability(coupon, request.getBookingAmount());

        double discount = calculateDiscount(coupon, request.getBookingAmount());
        double finalAmount = Math.max(0, request.getBookingAmount() - discount);

        ValidateCouponResponseDTO response = new ValidateCouponResponseDTO();
        response.setCode(coupon.getCode());
        response.setDiscountType(coupon.getDiscountType());
        response.setDiscountValue(coupon.getDiscountValue());
        response.setOriginalAmount(round2(request.getBookingAmount()));
        response.setDiscountAmount(round2(discount));
        response.setFinalAmount(round2(finalAmount));
        return response;
    }

    // -------------------------------------------------------------------------
    // Package-visible helpers — used by BookingServiceImpl
    // -------------------------------------------------------------------------

    public Coupon findAndValidateCoupon(String code, double bookingAmount) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon code '" + code + "' is not valid"));
        validateCouponApplicability(coupon, bookingAmount);
        return coupon;
    }

    public double calculateDiscount(Coupon coupon, double bookingAmount) {
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            return bookingAmount * (coupon.getDiscountValue() / 100.0);
        }
        return Math.min(coupon.getDiscountValue(), bookingAmount);
    }

    // -------------------------------------------------------------------------

    private void validateCouponApplicability(Coupon coupon, double bookingAmount) {
        if (!coupon.isActive()) {
            throw new BadRequestException("Coupon '" + coupon.getCode() + "' is no longer active");
        }
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Coupon '" + coupon.getCode() + "' has expired");
        }
        if (coupon.getMaxUses() > 0 && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new BadRequestException("Coupon '" + coupon.getCode() + "' has reached its maximum usage limit");
        }
        if (bookingAmount < coupon.getMinBookingAmount()) {
            throw new BadRequestException("This coupon requires a minimum booking amount of " + coupon.getMinBookingAmount());
        }
    }

    private Coupon getOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id " + id));
    }

    private CouponResponseDTO toDTO(Coupon coupon) {
        CouponResponseDTO dto = new CouponResponseDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setMinBookingAmount(coupon.getMinBookingAmount());
        dto.setMaxUses(coupon.getMaxUses());
        dto.setUsedCount(coupon.getUsedCount());
        dto.setExpiryDate(coupon.getExpiryDate());
        dto.setActive(coupon.isActive());
        dto.setCreatedAt(coupon.getCreatedAt());
        dto.setUpdatedAt(coupon.getUpdatedAt());
        return dto;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
