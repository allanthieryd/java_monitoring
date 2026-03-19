package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.UgcContentDto;
import com.example.dto.UgcContentRequest;
import com.example.entity.ContentStatus;
import com.example.service.UgcService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ugc")
public class UgcController {

    private final UgcService ugcService;

    public UgcController(UgcService ugcService) {
        this.ugcService = ugcService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UgcContentDto create(@Valid @RequestBody UgcContentRequest request) {
        return ugcService.create(request);
    }

    @GetMapping
    public List<UgcContentDto> list(@RequestParam(required = false) ContentStatus status) {
        return ugcService.list(status);
    }

    @PostMapping("/{id}/publish")
    public UgcContentDto publish(@PathVariable Long id) {
        return ugcService.publish(id);
    }

    @PostMapping("/{id}/archive")
    public UgcContentDto archive(@PathVariable Long id) {
        return ugcService.archive(id);
    }
}
