package com.example.dto;

import java.math.BigDecimal;

public class LeaderboardEntryDto {

    private Long profilId;
    private String name;
    private Integer rankPoints;
    private BigDecimal credits;

    public Long getProfilId() {
        return profilId;
    }

    public void setProfilId(Long profilId) {
        this.profilId = profilId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRankPoints() {
        return rankPoints;
    }

    public void setRankPoints(Integer rankPoints) {
        this.rankPoints = rankPoints;
    }

    public BigDecimal getCredits() {
        return credits;
    }

    public void setCredits(BigDecimal credits) {
        this.credits = credits;
    }
}
