package com.example.event;

import org.springframework.context.ApplicationEvent;

/**
 * Publié lorsqu'un match se termine.
 * Le domaine leaderboard peut écouter cet événement pour
 * recalculer les classements sans que MatchmakingService
 * ne connaisse LeaderboardService.
 */
public class MatchCompletedEvent extends ApplicationEvent {

    private final Long matchId;
    private final Long winnerId;
    private final Long loserId;

    public MatchCompletedEvent(Object source, Long matchId, Long winnerId, Long loserId) {
        super(source);
        this.matchId = matchId;
        this.winnerId = winnerId;
        this.loserId = loserId;
    }

    public Long getMatchId() { return matchId; }
    public Long getWinnerId() { return winnerId; }
    public Long getLoserId() { return loserId; }
}
