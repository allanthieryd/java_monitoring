package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.ModerationReport;
import com.example.entity.ReportStatus;

public interface ModerationReportRepository extends JpaRepository<ModerationReport, Long> {

    List<ModerationReport> findTop100ByOrderByCreatedAtDesc();

    List<ModerationReport> findTop100ByStatusOrderByCreatedAtDesc(ReportStatus status);
}
