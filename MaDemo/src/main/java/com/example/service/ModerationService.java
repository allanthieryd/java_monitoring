package com.example.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.ModerationReportDto;
import com.example.dto.ModerationReportRequest;
import com.example.entity.ModerationReport;
import com.example.entity.ReportStatus;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.ModerationReportRepository;
import com.example.repository.ProfilRepository;
import com.example.repository.UgcContentRepository;

@Service
public class ModerationService {

    private final ModerationReportRepository moderationReportRepository;
    private final UgcContentRepository ugcContentRepository;
    private final ProfilRepository profilRepository;

    public ModerationService(
            ModerationReportRepository moderationReportRepository,
            UgcContentRepository ugcContentRepository,
            ProfilRepository profilRepository) {
        this.moderationReportRepository = moderationReportRepository;
        this.ugcContentRepository = ugcContentRepository;
        this.profilRepository = profilRepository;
    }

    @Transactional
    public ModerationReportDto createReport(ModerationReportRequest request) {
        ugcContentRepository.findById(request.getContentId())
                .orElseThrow(() -> new ResourceNotFoundException("Contenu introuvable avec id " + request.getContentId()));

        profilRepository.findById(request.getReporterProfilId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profil reporteur introuvable avec id " + request.getReporterProfilId()));

        ModerationReport report = new ModerationReport();
        report.setContentId(request.getContentId());
        report.setReporterProfilId(request.getReporterProfilId());
        report.setReason(request.getReason());
        report.setStatus(ReportStatus.OPEN);

        return toDto(moderationReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ModerationReportDto> listReports(ReportStatus status) {
        if (status == null) {
            return moderationReportRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
        }

        return moderationReportRepository.findTop100ByStatusOrderByCreatedAtDesc(status).stream().map(this::toDto).toList();
    }

    @Transactional
    public ModerationReportDto resolveReport(Long id) {
        ModerationReport report = findById(id);
        report.setStatus(ReportStatus.RESOLVED);
        return toDto(moderationReportRepository.save(report));
    }

    @Transactional
    public ModerationReportDto rejectReport(Long id) {
        ModerationReport report = findById(id);
        report.setStatus(ReportStatus.REJECTED);
        return toDto(moderationReportRepository.save(report));
    }

    private ModerationReport findById(Long id) {
        return moderationReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable avec id " + id));
    }

    private ModerationReportDto toDto(ModerationReport report) {
        ModerationReportDto dto = new ModerationReportDto();
        dto.setId(report.getId());
        dto.setContentId(report.getContentId());
        dto.setReporterProfilId(report.getReporterProfilId());
        dto.setReason(report.getReason());
        dto.setStatus(report.getStatus());
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }
}
