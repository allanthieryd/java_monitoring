package com.example.metrics;

import com.example.entity.MatchStatus;
import com.example.repository.MatchSessionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * Métriques orientées expérience joueur (UX), exposées via Prometheus.
 *
 * Distinction avec les métriques infra (CPU, RAM, IOPS) :
 * ces indicateurs mesurent ce que ressent le joueur, pas l'état de la machine.
 *
 * Métriques exposées :
 *  - match_abandonment_rate  : % de matchs annulés vs total → indique si
 *    les joueurs quittent les parties (problème de UX ou de stabilité réseau)
 *  - match_completion_rate   : % de matchs terminés correctement → santé
 *    du flux de jeu de bout en bout
 *  - match_open_ratio        : % de matchs en attente → charge en temps réel
 */
@Component
public class PlayerExperienceMetrics implements MeterBinder {

    private final MatchSessionRepository matchSessionRepository;

    public PlayerExperienceMetrics(MatchSessionRepository matchSessionRepository) {
        this.matchSessionRepository = matchSessionRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {

        Gauge.builder("match_abandonment_rate", matchSessionRepository, repo -> {
                    long total = repo.count();
                    if (total == 0) return 0.0;
                    return (double) repo.countByStatus(MatchStatus.CANCELED) / total * 100;
                })
                .description("Taux d'abandon des matchs en % (indicateur UX joueur)")
                .baseUnit("percent")
                .register(registry);

        Gauge.builder("match_completion_rate", matchSessionRepository, repo -> {
                    long total = repo.count();
                    if (total == 0) return 0.0;
                    return (double) repo.countByStatus(MatchStatus.COMPLETED) / total * 100;
                })
                .description("Taux de completion des matchs en % (indicateur sante du flux de jeu)")
                .baseUnit("percent")
                .register(registry);

        Gauge.builder("match_open_ratio", matchSessionRepository, repo -> {
                    long total = repo.count();
                    if (total == 0) return 0.0;
                    return (double) repo.countByStatus(MatchStatus.OPEN) / total * 100;
                })
                .description("Part des matchs encore en cours en % (charge temps reel)")
                .baseUnit("percent")
                .register(registry);
    }
}
