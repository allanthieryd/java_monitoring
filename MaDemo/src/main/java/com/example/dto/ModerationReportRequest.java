package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ModerationReportRequest {

    @NotNull(message = "contentId est obligatoire")
    private Long contentId;

    @NotNull(message = "reporterProfilId est obligatoire")
    private Long reporterProfilId;

    @NotBlank(message = "reason est obligatoire")
    @Size(max = 250, message = "reason est trop long")
    private String reason;

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
}
