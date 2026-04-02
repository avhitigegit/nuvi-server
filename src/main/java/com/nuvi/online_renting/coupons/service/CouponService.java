package com.nuvi.online_renting.coupons.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.coupons.dto.CouponRequestDTO;
import com.nuvi.online_renting.coupons.dto.CouponResponseDTO;
import com.nuvi.online_renting.coupons.dto.ValidateCouponRequestDTO;
import com.nuvi.online_renting.coupons.dto.ValidateCouponResponseDTO;
import org.springframework.data.domain.Pageable;

public interface CouponService {

    CouponResponseDTO createCoupon(CouponRequestDTO request);

    PagedResponse<CouponResponseDTO> getAllCoupons(Pageable pageable);

    CouponResponseDTO getCoupon(Long id);

    CouponResponseDTO deactivateCoupon(Long id);

    ValidateCouponResponseDTO validateCoupon(ValidateCouponRequestDTO request);
}
