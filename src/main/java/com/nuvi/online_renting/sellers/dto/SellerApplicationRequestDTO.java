package com.nuvi.online_renting.sellers.dto;

import jakarta.validation.constraints.NotBlank;

public class SellerApplicationRequestDTO {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Business address is required")
    private String address;

    @NotBlank(message = "ID number is required")
    private String idNumber;

    private String bankAccount;

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }
}
