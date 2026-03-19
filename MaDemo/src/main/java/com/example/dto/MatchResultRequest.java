package com.example.dto;

import jakarta.validation.constraints.NotNull;

public class MatchResultRequest {

    @NotNull(message = "winnerId est obligatoire")
    private Long winnerId;

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }
}
