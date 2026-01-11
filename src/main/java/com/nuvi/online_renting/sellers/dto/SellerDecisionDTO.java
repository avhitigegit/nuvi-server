package com.nuvi.online_renting.sellers.dto;

import com.nuvi.online_renting.common.enums.SellerStatus;
import jakarta.validation.constraints.NotNull;

public class SellerDecisionDTO {

    @NotNull
    private SellerStatus status; // APPROVED or REJECTED or REQUEST_INFO
    private String comment;

    public SellerStatus getStatus() {
        return status;
    }

    public void setStatus(SellerStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
