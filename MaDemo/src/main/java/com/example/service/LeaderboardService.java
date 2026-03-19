package com.example.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.LeaderboardEntryDto;
import com.example.entity.Profil;
import com.example.repository.ProfilRepository;

@Service
public class LeaderboardService {

    private final ProfilRepository profilRepository;

    public LeaderboardService(ProfilRepository profilRepository) {
        this.profilRepository = profilRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> topPlayers() {
        return profilRepository.findTop20ByOrderByRankPointsDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private LeaderboardEntryDto toDto(Profil profil) {
        LeaderboardEntryDto dto = new LeaderboardEntryDto();
        dto.setProfilId(profil.getId());
        dto.setName(profil.getName());
        dto.setRankPoints(profil.getRankPoints());
        dto.setCredits(profil.getCredits());
        return dto;
    }
}
