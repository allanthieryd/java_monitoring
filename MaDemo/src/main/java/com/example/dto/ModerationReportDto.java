package com.example.dto;

import java.time.Instant;

import com.example.entity.ReportStatus;

public class ModerationReportDto {

    private Long id;
    private Long contentId;
    private Long reporterProfilId;
    private String reason;
    private ReportStatus status;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public Long getReporterProfilId() {
        return reporterProfilId;
    }

    public void setReporterProfilId(Long reporterProfilId) {
        this.reporterProfilId = reporterProfilId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
