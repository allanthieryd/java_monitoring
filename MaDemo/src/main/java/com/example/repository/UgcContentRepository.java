package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.ContentStatus;
import com.example.entity.UgcContent;

public interface UgcContentRepository extends JpaRepository<UgcContent, Long> {

    List<UgcContent> findTop100ByOrderByCreatedAtDesc();

    List<UgcContent> findTop100ByStatusOrderByCreatedAtDesc(ContentStatus status);
}
