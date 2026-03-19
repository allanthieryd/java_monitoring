package com.example.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.MatchCreateRequest;
import com.example.dto.MatchDto;
import com.example.dto.MatchResultRequest;
import com.example.entity.MatchSession;
import com.example.entity.MatchStatus;
import com.example.entity.Profil;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.MatchSessionRepository;
import com.example.repository.ProfilRepository;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MatchmakingService {

    private final MatchSessionRepository matchSessionRepository;
    private final ProfilRepository profilRepository;
    private final Counter createdMatchesCounter;
    private final Counter completedMatchesCounter;

    public MatchmakingService(
            MatchSessionRepository matchSessionRepository,
            ProfilRepository profilRepository,
            MeterRegistry meterRegistry) {
        this.matchSessionRepository = matchSessionRepository;
        this.profilRepository = profilRepository;
        this.createdMatchesCounter = Counter.builder("match_created_total")
            .description("Nombre total de matchs crees")
            .register(meterRegistry);
        this.completedMatchesCounter = Counter.builder("match_completed_total")
                .description("Nombre total de matchs completes")
                .register(meterRegistry);
    }

    @Transactional
    @Timed(value = "service.match.create")
    public MatchDto createMatch(MatchCreateRequest request) {
        if (request.getPlayerOneId().equals(request.getPlayerTwoId())) {
            throw new IllegalArgumentException("Un joueur ne peut pas jouer contre lui-meme");
        }

        getProfilOrThrow(request.getPlayerOneId());
        getProfilOrThrow(request.getPlayerTwoId());

        MatchSession match = new MatchSession();
        match.setPlayerOneId(request.getPlayerOneId());
        match.setPlayerTwoId(request.getPlayerTwoId());

        MatchSession saved = matchSessionRepository.save(match);
        createdMatchesCounter.increment();
        return toDto(saved);
    }

    @Transactional
    @Timed(value = "service.match.complete")
    public MatchDto completeMatch(Long matchId, MatchResultRequest request) {
        MatchSession match = matchSessionRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match introuvable avec id " + matchId));

        if (match.getStatus() != MatchStatus.OPEN) {
            throw new IllegalArgumentException("Le match n'est pas dans un etat modifiable");
        }

        Long winnerId = request.getWinnerId();
        if (!winnerId.equals(match.getPlayerOneId()) && !winnerId.equals(match.getPlayerTwoId())) {
            throw new IllegalArgumentException("Le vainqueur doit etre un des deux joueurs du match");
        }

        Long loserId = winnerId.equals(match.getPlayerOneId()) ? match.getPlayerTwoId() : match.getPlayerOneId();
        Profil winner = getProfilOrThrow(winnerId);
        Profil loser = getProfilOrThrow(loserId);

        winner.setRankPoints(winner.getRankPoints() + 25);
        loser.setRankPoints(Math.max(0, loser.getRankPoints() - 20));

        profilRepository.save(winner);
        profilRepository.save(loser);

        match.setWinnerId(winnerId);
        match.setStatus(MatchStatus.COMPLETED);
        match.setEndedAt(Instant.now());

        completedMatchesCounter.increment();
        return toDto(matchSessionRepository.save(match));
    }

    @Transactional(readOnly = true)
    public MatchDto getMatch(Long id) {
        MatchSession match = matchSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match introuvable avec id " + id));
        return toDto(match);
    }

    @Transactional(readOnly = true)
    public List<MatchDto> listMatches() {
        return matchSessionRepository.findTop50ByOrderByStartedAtDesc().stream().map(this::toDto).toList();
    }

    private Profil getProfilOrThrow(Long id) {
        return profilRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable avec id " + id));
    }

    private MatchDto toDto(MatchSession match) {
        MatchDto dto = new MatchDto();
        dto.setId(match.getId());
        dto.setPlayerOneId(match.getPlayerOneId());
        dto.setPlayerTwoId(match.getPlayerTwoId());
        dto.setWinnerId(match.getWinnerId());
        dto.setStatus(match.getStatus());
        dto.setStartedAt(match.getStartedAt());
        dto.setEndedAt(match.getEndedAt());
        return dto;
    }
}
