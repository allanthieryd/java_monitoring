package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.MatchCreateRequest;
import com.example.dto.MatchDto;
import com.example.dto.MatchResultRequest;
import com.example.service.MatchmakingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchmakingService matchmakingService;

    public MatchController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDto create(@Valid @RequestBody MatchCreateRequest request) {
        return matchmakingService.createMatch(request);
    }

    @PostMapping("/{id}/complete")
    public MatchDto complete(@PathVariable Long id, @Valid @RequestBody MatchResultRequest request) {
        return matchmakingService.completeMatch(id, request);
    }

    @GetMapping
    public List<MatchDto> list() {
        return matchmakingService.listMatches();
    }

    @GetMapping("/{id}")
    public MatchDto get(@PathVariable Long id) {
        return matchmakingService.getMatch(id);
    }
}
