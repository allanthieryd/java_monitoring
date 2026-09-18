package com.example.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre de rate limiting par adresse IP.
 *
 * Deux fenêtres de limitation :
 *  - Endpoint d'authentification (/api/v1/auth/**) : 10 requêtes/minute
 *    → protection contre le brute-force des identifiants
 *  - Tous les autres endpoints API : 100 requêtes/minute
 *    → protection contre le scraping et les abus de l'API
 *
 * Implémentation : token bucket (Bucket4j) en mémoire locale.
 * Pour un déploiement multi-instances, remplacer le ConcurrentHashMap
 * par un backend distribué (Redis via bucket4j-redis).
 *
 * Les seuils sont pilotés par les propriétés {@code ratelimit.*} : les tests
 * de charge k6 les relèvent (ou désactivent le filtre) pour mesurer
 * l'application et non le rate limiter.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    public static final int DEFAULT_API_LIMIT_PER_MINUTE = 100;
    public static final int DEFAULT_AUTH_LIMIT_PER_MINUTE = 10;

    private final boolean enabled;
    private final int apiLimitPerMinute;
    private final int authLimitPerMinute;

    /** Un bucket par IP pour les endpoints API généraux */
    private final ConcurrentHashMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    /** Un bucket par IP pour les endpoints d'authentification */
    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        this(true, DEFAULT_API_LIMIT_PER_MINUTE, DEFAULT_AUTH_LIMIT_PER_MINUTE);
    }

    public RateLimitFilter(boolean enabled, int apiLimitPerMinute, int authLimitPerMinute) {
        this.enabled = enabled;
        this.apiLimitPerMinute = apiLimitPerMinute;
        this.authLimitPerMinute = authLimitPerMinute;
        if (!enabled) {
            log.warn("[RATE-LIMIT] Filtre désactivé (ratelimit.enabled=false) — à réserver aux tests de charge");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Ne rate-limite que les endpoints API
        if (!enabled || !uri.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        boolean isAuthEndpoint = uri.startsWith("/api/v1/auth/");

        Bucket bucket = isAuthEndpoint
                ? authBuckets.computeIfAbsent(ip, k -> buildBucket(authLimitPerMinute))
                : apiBuckets.computeIfAbsent(ip, k -> buildBucket(apiLimitPerMinute));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            int limit = isAuthEndpoint ? authLimitPerMinute : apiLimitPerMinute;
            log.warn("[RATE-LIMIT] IP {} bloquée sur {} (limite : {}/min)", ip, uri, limit);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                    "\"message\":\"Limite de requetes atteinte. Reessayez dans une minute.\"}");
        }
    }

    private Bucket buildBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Résout l'IP réelle du client en tenant compte des proxies/load balancers
     * (header X-Forwarded-For standard).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
