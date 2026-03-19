package com.example.dto;

import java.math.BigDecimal;

import com.example.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class WalletTransactionRequest {

    @NotNull(message = "profilId est obligatoire")
    private Long profilId;

    @NotNull(message = "type est obligatoire")
    private TransactionType type;

    @NotNull(message = "amount est obligatoire")
    @DecimalMin(value = "0.01", message = "amount doit etre > 0")
    private BigDecimal amount;

    @NotBlank(message = "reason est obligatoire")
    @Size(max = 200, message = "reason est trop long")
    private String reason;

    public Long getProfilId() {
        return profilId;
    }

    public void setProfilId(Long profilId) {
        this.profilId = profilId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
