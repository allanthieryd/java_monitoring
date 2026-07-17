package com.example.event.listener;

import com.example.entity.ContentStatus;
import com.example.event.ModerationReportCreatedEvent;
import com.example.repository.ModerationReportRepository;
import com.example.repository.UgcContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applique les règles automatiques de modération sans polluer ModerationService.
 * Exemple : un contenu signalé 3 fois ou plus est automatiquement archivé.
 *
 * Ce listener illustre le découplage événementiel : ModerationService ne sait pas
 * qu'une règle d'auto-archivage existe — il publie l'événement, ce composant réagit.
 */
@Component
public class ModerationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ModerationEventListener.class);
    private static final int AUTO_ARCHIVE_THRESHOLD = 3;

    private final ModerationReportRepository moderationReportRepository;
    private final UgcContentRepository ugcContentRepository;

    public ModerationEventListener(ModerationReportRepository moderationReportRepository,
                                   UgcContentRepository ugcContentRepository) {
        this.moderationReportRepository = moderationReportRepository;
        this.ugcContentRepository = ugcContentRepository;
    }

    @EventListener
    @Transactional
    public void onReportCreated(ModerationReportCreatedEvent event) {
        long reportCount = moderationReportRepository.countByContentId(event.getContentId());

        if (reportCount >= AUTO_ARCHIVE_THRESHOLD) {
            ugcContentRepository.findById(event.getContentId()).ifPresent(content -> {
                if (content.getStatus() != ContentStatus.ARCHIVED) {
                    content.setStatus(ContentStatus.ARCHIVED);
                    ugcContentRepository.save(content);
                    log.warn("[MODERATION] Contenu {} auto-archivé après {} signalements",
                            event.getContentId(), reportCount);
                }
            });
        }
    }
}
