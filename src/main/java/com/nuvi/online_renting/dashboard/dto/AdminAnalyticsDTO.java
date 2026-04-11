package com.nuvi.online_renting.dashboard.dto;

import java.util.List;

/**
 * Admin analytics response — time-series data for dashboard charts.
 * All monetary values are in LKR.
 */
public class AdminAnalyticsDTO {

    // ─── Platform totals ─────────────────────────────────────────────────────
    private long totalUsers;
    private long totalSellers;
    private long totalItems;
    private long totalBookings;
    private double totalRevenue;          // sum of totalAmount for COMPLETED bookings

    // ─── Booking counts by status ─────────────────────────────────────────────
    private long pendingBookings;
    private long confirmedBookings;
    private long completedBookings;
    private long cancelledBookings;

    // ─── Time-series (daily, last N days) ────────────────────────────────────
    private List<DailyStatDTO> dailyBookings;   // bookings created per day
    private List<DailyStatDTO> dailyRevenue;    // revenue (COMPLETED) per day

    // ─── Nested DTO ──────────────────────────────────────────────────────────
    public static class DailyStatDTO {
        private String date;    // ISO date: "2025-11-01"
        private double value;   // count or revenue

        public DailyStatDTO(String date, double value) {
            this.date = date;
            this.value = value;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalSellers() { return totalSellers; }
    public void setTotalSellers(long totalSellers) { this.totalSellers = totalSellers; }

    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public long getPendingBookings() { return pendingBookings; }
    public void setPendingBookings(long pendingBookings) { this.pendingBookings = pendingBookings; }

    public long getConfirmedBookings() { return confirmedBookings; }
    public void setConfirmedBookings(long confirmedBookings) { this.confirmedBookings = confirmedBookings; }

    public long getCompletedBookings() { return completedBookings; }
    public void setCompletedBookings(long completedBookings) { this.completedBookings = completedBookings; }

    public long getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(long cancelledBookings) { this.cancelledBookings = cancelledBookings; }

    public List<DailyStatDTO> getDailyBookings() { return dailyBookings; }
    public void setDailyBookings(List<DailyStatDTO> dailyBookings) { this.dailyBookings = dailyBookings; }

    public List<DailyStatDTO> getDailyRevenue() { return dailyRevenue; }
    public void setDailyRevenue(List<DailyStatDTO> dailyRevenue) { this.dailyRevenue = dailyRevenue; }
}
