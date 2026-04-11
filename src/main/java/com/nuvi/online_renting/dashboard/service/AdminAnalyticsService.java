package com.nuvi.online_renting.dashboard.service;

import com.nuvi.online_renting.dashboard.dto.AdminAnalyticsDTO;

public interface AdminAnalyticsService {
    AdminAnalyticsDTO getAnalytics(int days);
}
