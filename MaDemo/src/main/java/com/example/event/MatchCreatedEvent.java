package com.example.event;

import org.springframework.context.ApplicationEvent;

/**
 * Publié lorsqu'une nouvelle session de match est créée.
 * Permet aux autres domaines (leaderboard, économie, monitoring)
 * de réagir sans couplage direct au service de matchmaking.
 */
public class MatchCreatedEvent extends ApplicationEvent {

    private final Long matchId;
    private final Long playerOneId;
    private final Long playerTwoId;

    public MatchCreatedEvent(Object source, Long matchId, Long playerOneId, Long playerTwoId) {
        super(source);
        this.matchId = matchId;
        this.playerOneId = playerOneId;
        this.playerTwoId = playerTwoId;
    }

    public Long getMatchId() { return matchId; }
    public Long getPlayerOneId() { return playerOneId; }
    public Long getPlayerTwoId() { return playerTwoId; }
}
