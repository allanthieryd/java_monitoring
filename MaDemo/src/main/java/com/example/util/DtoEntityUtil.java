package com.example.util;

import java.math.BigDecimal;

import com.example.dto.ProfilDto;
import com.example.entity.Profil;

public class DtoEntityUtil {

    private DtoEntityUtil() {
    }

    public static Profil toEntity(ProfilDto profilDto) {
        Profil profil = new Profil();
        profil.setName(profilDto.getName());
        profil.setEmail(profilDto.getEmail());
        profil.setRankPoints(profilDto.getRankPoints() == null ? 1000 : profilDto.getRankPoints());
        profil.setCredits(profilDto.getCredits() == null ? BigDecimal.ZERO : profilDto.getCredits());
        return profil;
    }

    public static ProfilDto toDto(Profil profil) {
        ProfilDto dto = new ProfilDto();
        dto.setId(profil.getId());
        dto.setName(profil.getName());
        dto.setEmail(profil.getEmail());
        dto.setRankPoints(profil.getRankPoints());
        dto.setCredits(profil.getCredits());
        return dto;
    }
}
