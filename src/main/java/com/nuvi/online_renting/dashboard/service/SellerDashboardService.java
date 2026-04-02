package com.nuvi.online_renting.dashboard.service;

import com.nuvi.online_renting.dashboard.dto.SellerDashboardDTO;

public interface SellerDashboardService {

    SellerDashboardDTO getDashboard(Long sellerId);
}
