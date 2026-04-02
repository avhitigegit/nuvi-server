package com.nuvi.online_renting.dashboard.dto;

import java.util.List;

public class SellerDashboardDTO {

    // ─── Item summary ─────────────────────────────────────────────────────────
    private long totalItems;
    private long activeItems;        // available = true
    private long inactiveItems;      // available = false

    // ─── Seller rating ────────────────────────────────────────────────────────
    private double averageRating;
    private int totalReviews;

    // ─── Booking summary ─────────────────────────────────────────────────────
    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long completedBookings;
    private long cancelledBookings;
    private long bookingsThisMonth;
    private long bookingsThisWeek;

    // ─── Earnings ────────────────────────────────────────────────────────────
    private double totalEarnings;          // all-time, COMPLETED bookings only
    private double earningsThisMonth;
    private double earningsThisWeek;

    // ─── Top items ───────────────────────────────────────────────────────────
    private List<TopItemDTO> topItems;

    // ─── Nested DTO ──────────────────────────────────────────────────────────
    public static class TopItemDTO {
        private Long itemId;
        private String itemName;
        private long completedBookings;
        private double totalEarnings;

        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public long getCompletedBookings() { return completedBookings; }
        public void setCompletedBookings(long completedBookings) { this.completedBookings = completedBookings; }

        public double getTotalEarnings() { return totalEarnings; }
        public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }

    public long getActiveItems() { return activeItems; }
    public void setActiveItems(long activeItems) { this.activeItems = activeItems; }

    public long getInactiveItems() { return inactiveItems; }
    public void setInactiveItems(long inactiveItems) { this.inactiveItems = inactiveItems; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getPendingBookings() { return pendingBookings; }
    public void setPendingBookings(long pendingBookings) { this.pendingBookings = pendingBookings; }

    public long getConfirmedBookings() { return confirmedBookings; }
    public void setConfirmedBookings(long confirmedBookings) { this.confirmedBookings = confirmedBookings; }

    public long getCompletedBookings() { return completedBookings; }
    public void setCompletedBookings(long completedBookings) { this.completedBookings = completedBookings; }

    public long getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(long cancelledBookings) { this.cancelledBookings = cancelledBookings; }

    public long getBookingsThisMonth() { return bookingsThisMonth; }
    public void setBookingsThisMonth(long bookingsThisMonth) { this.bookingsThisMonth = bookingsThisMonth; }

    public long getBookingsThisWeek() { return bookingsThisWeek; }
    public void setBookingsThisWeek(long bookingsThisWeek) { this.bookingsThisWeek = bookingsThisWeek; }

    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }

    public double getEarningsThisMonth() { return earningsThisMonth; }
    public void setEarningsThisMonth(double earningsThisMonth) { this.earningsThisMonth = earningsThisMonth; }

    public double getEarningsThisWeek() { return earningsThisWeek; }
    public void setEarningsThisWeek(double earningsThisWeek) { this.earningsThisWeek = earningsThisWeek; }

    public List<TopItemDTO> getTopItems() { return topItems; }
    public void setTopItems(List<TopItemDTO> topItems) { this.topItems = topItems; }
}
