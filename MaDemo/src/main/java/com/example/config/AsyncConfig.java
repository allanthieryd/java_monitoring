package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

/**
 * Configuration de l'exécuteur asynchrone utilisé par @Async.
 *
 * Choix technique : Virtual Threads (Java 21 / Project Loom).
 * Justification :
 *  - Les listeners d'événements font essentiellement de l'I/O (logs, DB)
 *    → opérations bloquantes idéales pour les virtual threads
 *  - Pas de pool à dimensionner : la JVM crée/détruit les VT à la demande
 *  - Empreinte mémoire négligeable vs thread pool classique
 *  - Simplifie l'opérationnel (pas de tuning pool-size en prod)
 */
@Configuration
public class AsyncConfig {

    @Bean("taskExecutor")
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
