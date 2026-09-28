package com.festpass.dto;

import jakarta.validation.constraints.NotBlank;

public class ValidateTicketRequest {

    @NotBlank(message = "QR Code is required")
    private String qrCode;

    public ValidateTicketRequest() {
    }

    public ValidateTicketRequest(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
}
