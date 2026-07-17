package com.example.event;

import org.springframework.context.ApplicationEvent;

/**
 * Publié lorsqu'un signalement est soumis contre un contenu UGC.
 * Permet d'implémenter des règles automatiques (ex: archiver
 * un contenu après N signalements) sans polluer ModerationService.
 */
public class ModerationReportCreatedEvent extends ApplicationEvent {

    private final Long reportId;
    private final Long contentId;
    private final Long reporterProfilId;

    public ModerationReportCreatedEvent(Object source, Long reportId, Long contentId, Long reporterProfilId) {
        super(source);
        this.reportId = reportId;
        this.contentId = contentId;
        this.reporterProfilId = reporterProfilId;
    }

    public Long getReportId() { return reportId; }
    public Long getContentId() { return contentId; }
    public Long getReporterProfilId() { return reporterProfilId; }
}
