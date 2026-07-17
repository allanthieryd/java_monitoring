package com.example.MaDemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.dto.MatchCreateRequest;
import com.example.dto.MatchDto;
import com.example.dto.MatchResultRequest;
import com.example.entity.MatchSession;
import com.example.entity.MatchStatus;
import com.example.entity.Profil;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.MatchSessionRepository;
import com.example.repository.ProfilRepository;
import com.example.service.MatchmakingService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    private MatchSessionRepository matchSessionRepository;

    @Mock
    private ProfilRepository profilRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        matchmakingService = new MatchmakingService(matchSessionRepository, profilRepository, eventPublisher, new SimpleMeterRegistry());
    }

    @Test
    void createMatch_shouldReturnDto_whenPlayersExist() {
        Profil p1 = profil(1L);
        Profil p2 = profil(2L);
        when(profilRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(profilRepository.findById(2L)).thenReturn(Optional.of(p2));

        MatchSession saved = new MatchSession();
        saved.setId(10L);
        saved.setPlayerOneId(1L);
        saved.setPlayerTwoId(2L);
        when(matchSessionRepository.save(any())).thenReturn(saved);

        MatchCreateRequest req = new MatchCreateRequest();
        req.setPlayerOneId(1L);
        req.setPlayerTwoId(2L);

        MatchDto result = matchmakingService.createMatch(req);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(MatchStatus.OPEN);
    }

    @Test
    void createMatch_shouldThrow_whenSamePlayer() {
        MatchCreateRequest req = new MatchCreateRequest();
        req.setPlayerOneId(1L);
        req.setPlayerTwoId(1L);

        assertThatThrownBy(() -> matchmakingService.createMatch(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lui-meme");
    }

    @Test
    void createMatch_shouldThrow_whenPlayerNotFound() {
        when(profilRepository.findById(1L)).thenReturn(Optional.empty());

        MatchCreateRequest req = new MatchCreateRequest();
        req.setPlayerOneId(1L);
        req.setPlayerTwoId(2L);

        assertThatThrownBy(() -> matchmakingService.createMatch(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completeMatch_shouldUpdateRankPoints() {
        Profil winner = profil(1L);
        Profil loser = profil(2L);
        winner.setRankPoints(1000);
        loser.setRankPoints(1000);

        MatchSession match = openMatch(1L, 2L);
        match.setId(5L);

        when(matchSessionRepository.findById(5L)).thenReturn(Optional.of(match));
        when(profilRepository.findById(1L)).thenReturn(Optional.of(winner));
        when(profilRepository.findById(2L)).thenReturn(Optional.of(loser));
        when(profilRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MatchResultRequest req = new MatchResultRequest();
        req.setWinnerId(1L);

        MatchDto result = matchmakingService.completeMatch(5L, req);

        assertThat(result.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(result.getWinnerId()).isEqualTo(1L);
        assertThat(winner.getRankPoints()).isEqualTo(1025);
        assertThat(loser.getRankPoints()).isEqualTo(980);
    }

    @Test
    void completeMatch_shouldThrow_whenMatchAlreadyClosed() {
        MatchSession match = openMatch(1L, 2L);
        match.setId(5L);
        match.setStatus(MatchStatus.COMPLETED);

        when(matchSessionRepository.findById(5L)).thenReturn(Optional.of(match));

        MatchResultRequest req = new MatchResultRequest();
        req.setWinnerId(1L);

        assertThatThrownBy(() -> matchmakingService.completeMatch(5L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modifiable");
    }

    @Test
    void completeMatch_shouldThrow_whenWinnerNotInMatch() {
        MatchSession match = openMatch(1L, 2L);
        match.setId(5L);

        when(matchSessionRepository.findById(5L)).thenReturn(Optional.of(match));

        MatchResultRequest req = new MatchResultRequest();
        req.setWinnerId(99L);

        assertThatThrownBy(() -> matchmakingService.completeMatch(5L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vainqueur");
    }

    @Test
    void getMatch_shouldThrow_whenNotFound() {
        when(matchSessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchmakingService.getMatch(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- helpers ---

    private Profil profil(Long id) {
        Profil p = new Profil();
        p.setId(id);
        p.setName("Joueur " + id);
        p.setEmail("joueur" + id + "@test.fr");
        return p;
    }

    private MatchSession openMatch(Long p1, Long p2) {
        MatchSession m = new MatchSession();
        m.setPlayerOneId(p1);
        m.setPlayerTwoId(p2);
        m.setStatus(MatchStatus.OPEN);
        return m;
    }
}
