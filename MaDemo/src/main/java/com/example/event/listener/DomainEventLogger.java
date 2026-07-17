package com.example.event.listener;

import com.example.event.MatchCompletedEvent;
import com.example.event.MatchCreatedEvent;
import com.example.event.ModerationReportCreatedEvent;
import com.example.event.UgcPublishedEvent;
import com.example.event.WalletTransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener transversal de traçabilité : journalise chaque événement métier.
 *
 * @Async : les logs sont écrits dans un virtual thread séparé — le thread
 * du service appelant est libéré immédiatement sans attendre l'I/O de log.
 * Découplé de tous les domaines — peut être remplacé par un sink Kafka
 * ou un audit log persistant sans toucher aux services.
 */
@Component
public class DomainEventLogger {

    private static final Logger log = LoggerFactory.getLogger(DomainEventLogger.class);

    @Async
    @EventListener
    public void onMatchCreated(MatchCreatedEvent event) {
        log.info("[MATCH] Nouveau match créé — id={} joueur1={} joueur2={}",
                event.getMatchId(), event.getPlayerOneId(), event.getPlayerTwoId());
    }

    @Async
    @EventListener
    public void onMatchCompleted(MatchCompletedEvent event) {
        log.info("[MATCH] Match terminé — id={} vainqueur={} perdant={}",
                event.getMatchId(), event.getWinnerId(), event.getLoserId());
    }

    @Async
    @EventListener
    public void onUgcPublished(UgcPublishedEvent event) {
        log.info("[UGC] Contenu publié — id={} auteur={}",
                event.getContentId(), event.getAuthorProfilId());
    }

    @Async
    @EventListener
    public void onModerationReportCreated(ModerationReportCreatedEvent event) {
        log.warn("[MODERATION] Signalement soumis — reportId={} contentId={} reporter={}",
                event.getReportId(), event.getContentId(), event.getReporterProfilId());
    }

    @Async
    @EventListener
    public void onWalletTransaction(WalletTransactionEvent event) {
        log.info("[ECONOMY] Transaction — id={} profil={} type={} montant={}",
                event.getTransactionId(), event.getProfilId(), event.getType(), event.getAmount());
    }
}
