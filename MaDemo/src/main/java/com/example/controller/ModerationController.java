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

import com.example.dto.ModerationReportDto;
import com.example.dto.ModerationReportRequest;
import com.example.entity.ReportStatus;
import com.example.service.ModerationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/moderation/reports")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModerationReportDto create(@Valid @RequestBody ModerationReportRequest request) {
        return moderationService.createReport(request);
    }

    @GetMapping
    public List<ModerationReportDto> list(@RequestParam(required = false) ReportStatus status) {
        return moderationService.listReports(status);
    }

    @PostMapping("/{id}/resolve")
    public ModerationReportDto resolve(@PathVariable Long id) {
        return moderationService.resolveReport(id);
    }

    @PostMapping("/{id}/reject")
    public ModerationReportDto reject(@PathVariable Long id) {
        return moderationService.rejectReport(id);
    }
}
