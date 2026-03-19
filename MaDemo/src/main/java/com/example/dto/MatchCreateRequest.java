package com.example.dto;

import jakarta.validation.constraints.NotNull;

public class MatchCreateRequest {

    @NotNull(message = "playerOneId est obligatoire")
    private Long playerOneId;

    @NotNull(message = "playerTwoId est obligatoire")
    private Long playerTwoId;

    public Long getPlayerOneId() {
        return playerOneId;
    }

    public void setPlayerOneId(Long playerOneId) {
        this.playerOneId = playerOneId;
    }

    public Long getPlayerTwoId() {
        return playerTwoId;
    }

    public void setPlayerTwoId(Long playerTwoId) {
        this.playerTwoId = playerTwoId;
    }
}
