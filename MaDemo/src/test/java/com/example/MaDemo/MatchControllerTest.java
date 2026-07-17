package com.example.MaDemo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.controller.MatchController;
import com.example.dto.MatchDto;
import com.example.entity.MatchStatus;
import com.example.exception.ResourceNotFoundException;
import com.example.securite.JwtUtil;
import com.example.service.MatchmakingService;

@WebMvcTest(MatchController.class)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchmakingService matchmakingService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(roles = "USER")
    void createMatch_shouldReturn201() throws Exception {
        MatchDto dto = matchDto(1L, 1L, 2L, MatchStatus.OPEN);
        when(matchmakingService.createMatch(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerOneId\":1,\"playerTwoId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createMatch_shouldReturn400_whenSamePlayer() throws Exception {
        when(matchmakingService.createMatch(any()))
                .thenThrow(new IllegalArgumentException("Un joueur ne peut pas jouer contre lui-meme"));

        mockMvc.perform(post("/api/v1/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerOneId\":1,\"playerTwoId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMatch_shouldReturn200() throws Exception {
        when(matchmakingService.getMatch(1L)).thenReturn(matchDto(1L, 1L, 2L, MatchStatus.OPEN));

        mockMvc.perform(get("/api/v1/matches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMatch_shouldReturn404_whenNotFound() throws Exception {
        when(matchmakingService.getMatch(99L))
                .thenThrow(new ResourceNotFoundException("Match introuvable avec id 99"));

        mockMvc.perform(get("/api/v1/matches/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listMatches_shouldReturn200() throws Exception {
        when(matchmakingService.listMatches()).thenReturn(List.of(matchDto(1L, 1L, 2L, MatchStatus.OPEN)));

        mockMvc.perform(get("/api/v1/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void completeMatch_shouldReturn200() throws Exception {
        MatchDto dto = matchDto(1L, 1L, 2L, MatchStatus.COMPLETED);
        dto.setWinnerId(1L);
        when(matchmakingService.completeMatch(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/matches/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winnerId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.winnerId").value(1));
    }

    @Test
    void createMatch_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerOneId\":1,\"playerTwoId\":2}"))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private MatchDto matchDto(Long id, Long p1, Long p2, MatchStatus status) {
        MatchDto dto = new MatchDto();
        dto.setId(id);
        dto.setPlayerOneId(p1);
        dto.setPlayerTwoId(p2);
        dto.setStatus(status);
        return dto;
    }
}
