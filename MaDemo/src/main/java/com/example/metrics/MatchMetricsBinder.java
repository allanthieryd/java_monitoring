package com.example.metrics;

import org.springframework.stereotype.Component;

import com.example.entity.MatchStatus;
import com.example.repository.MatchSessionRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

@Component
public class MatchMetricsBinder implements MeterBinder {

    private final MatchSessionRepository matchSessionRepository;

    public MatchMetricsBinder(MatchSessionRepository matchSessionRepository) {
        this.matchSessionRepository = matchSessionRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("match_sessions", matchSessionRepository, MatchSessionRepository::count)
                .description("Nombre total de matchs")
                .tag("status", "ALL")
                .register(registry);

        Gauge.builder("match_sessions", matchSessionRepository,
                repository -> repository.countByStatus(MatchStatus.OPEN))
                .description("Nombre de matchs par statut")
                .tag("status", MatchStatus.OPEN.name())
                .register(registry);

        Gauge.builder("match_sessions", matchSessionRepository,
                repository -> repository.countByStatus(MatchStatus.COMPLETED))
                .description("Nombre de matchs par statut")
                .tag("status", MatchStatus.COMPLETED.name())
                .register(registry);

        Gauge.builder("match_sessions", matchSessionRepository,
                repository -> repository.countByStatus(MatchStatus.CANCELED))
                .description("Nombre de matchs par statut")
                .tag("status", MatchStatus.CANCELED.name())
                .register(registry);
    }
}
