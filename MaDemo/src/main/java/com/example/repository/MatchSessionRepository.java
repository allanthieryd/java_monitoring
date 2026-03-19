package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.MatchSession;
import com.example.entity.MatchStatus;

public interface MatchSessionRepository extends JpaRepository<MatchSession, Long> {

    List<MatchSession> findTop50ByOrderByStartedAtDesc();

    long countByStatus(MatchStatus status);
}
