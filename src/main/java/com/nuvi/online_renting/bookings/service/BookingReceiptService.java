package com.nuvi.online_renting.bookings.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nuvi.online_renting.bookings.model.Booking;
import com.nuvi.online_renting.bookings.repository.BookingRepository;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.users.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class BookingReceiptService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final BookingRepository bookingRepository;
    private final AuthenticationFacade authFacade;

    public BookingReceiptService(BookingRepository bookingRepository,
                                 AuthenticationFacade authFacade) {
        this.bookingRepository = bookingRepository;
        this.authFacade = authFacade;
    }

    @Transactional(readOnly = true)
    public byte[] generateReceipt(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin      = currentUser.getRole() == Role.ADMIN;
        boolean isRenter     = booking.getUser().getId().equals(currentUser.getId());
        boolean isItemOwner  = booking.getItem().getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isRenter && !isItemOwner) {
            throw new ForbiddenException("You are not allowed to download a receipt for this booking");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ─── Fonts ─────────────────────────────────────────────────────────
            Font titleFont  = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(30, 30, 80));
            Font headFont   = new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY);
            Font bodyFont   = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font labelFont  = new Font(Font.HELVETICA, 9,  Font.BOLD,   new Color(80, 80, 80));
            Font valueFont  = new Font(Font.HELVETICA, 9,  Font.NORMAL, Color.DARK_GRAY);
            Font smallGray  = new Font(Font.HELVETICA, 8,  Font.NORMAL, Color.GRAY);

            // ─── Header ────────────────────────────────────────────────────────
            Paragraph title = new Paragraph("NUVI Rental Platform", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph sub = new Paragraph("Booking Receipt", headFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            doc.add(sub);

            addDivider(doc);

            // ─── Booking Info table ────────────────────────────────────────────
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{35, 65});
            infoTable.setSpacingBefore(12);
            infoTable.setSpacingAfter(12);

            addRow(infoTable, "Booking ID",   "#" + booking.getId(),              labelFont, valueFont);
            addRow(infoTable, "Status",       booking.getStatus(),                 labelFont, valueFont);
            addRow(infoTable, "Renter",       booking.getUser().getName(),          labelFont, valueFont);
            addRow(infoTable, "Renter Email", booking.getUser().getEmail(),         labelFont, valueFont);
            addRow(infoTable, "Item",         booking.getItem().getName(),          labelFont, valueFont);
            addRow(infoTable, "Seller",       booking.getItem().getSeller().getName(), labelFont, valueFont);
            addRow(infoTable, "Rental Start", booking.getStartDate().format(DATE_FMT),  labelFont, valueFont);
            addRow(infoTable, "Rental End",   booking.getEndDate().format(DATE_FMT),    labelFont, valueFont);
            long days = booking.getEndDate().toEpochDay() - booking.getStartDate().toEpochDay();
            addRow(infoTable, "Duration",     days + " day" + (days == 1 ? "" : "s"), labelFont, valueFont);
            addRow(infoTable, "Price/Day",    "LKR " + String.format("%.2f", booking.getItem().getPricePerDay()), labelFont, valueFont);

            if (booking.getAppliedCouponCode() != null) {
                addRow(infoTable, "Coupon Code",    booking.getAppliedCouponCode(),                     labelFont, valueFont);
                addRow(infoTable, "Discount",       "LKR " + String.format("%.2f", booking.getDiscountAmount()), labelFont, valueFont);
            }

            addRow(infoTable, "Total Amount", "LKR " + String.format("%.2f", booking.getTotalAmount()),
                    new Font(Font.HELVETICA, 9, Font.BOLD, new Color(20, 100, 20)),
                    new Font(Font.HELVETICA, 9, Font.BOLD, new Color(20, 100, 20)));

            if (booking.getReturnedAt() != null) {
                addRow(infoTable, "Returned At", booking.getReturnedAt().format(DATETIME_FMT), labelFont, valueFont);
            }
            if (booking.getReturnNote() != null) {
                addRow(infoTable, "Return Note", booking.getReturnNote(), labelFont, valueFont);
            }

            if (booking.getCreatedAt() != null) {
                addRow(infoTable, "Booked On", booking.getCreatedAt().format(DATETIME_FMT), labelFont, valueFont);
            }

            doc.add(infoTable);

            addDivider(doc);

            // ─── Footer ────────────────────────────────────────────────────────
            Paragraph footer = new Paragraph(
                    "This document was generated by the NUVI Rental Platform. " +
                    "For queries contact support@nuvi.lk", smallGray);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(16);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate booking receipt PDF", e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(new Color(220, 220, 220));
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new Color(248, 248, 252));

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(new Color(220, 220, 220));
        valueCell.setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addDivider(Document doc) throws DocumentException {
        Paragraph line = new Paragraph(" ");
        line.setSpacingBefore(2);
        line.setSpacingAfter(2);
        doc.add(line);
    }
}
