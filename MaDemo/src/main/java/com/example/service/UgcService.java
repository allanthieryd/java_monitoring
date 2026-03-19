package com.example.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.UgcContentDto;
import com.example.dto.UgcContentRequest;
import com.example.entity.ContentStatus;
import com.example.entity.UgcContent;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.ProfilRepository;
import com.example.repository.UgcContentRepository;

import io.micrometer.core.annotation.Timed;

@Service
public class UgcService {

    private final UgcContentRepository ugcContentRepository;
    private final ProfilRepository profilRepository;

    public UgcService(UgcContentRepository ugcContentRepository, ProfilRepository profilRepository) {
        this.ugcContentRepository = ugcContentRepository;
        this.profilRepository = profilRepository;
    }

    @Transactional
    @Timed(value = "service.ugc.create")
    public UgcContentDto create(UgcContentRequest request) {
        profilRepository.findById(request.getAuthorProfilId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profil auteur introuvable avec id " + request.getAuthorProfilId()));

        UgcContent content = new UgcContent();
        content.setAuthorProfilId(request.getAuthorProfilId());
        content.setTitle(request.getTitle());
        content.setBody(request.getBody());
        content.setStatus(ContentStatus.DRAFT);

        return toDto(ugcContentRepository.save(content));
    }

    @Transactional(readOnly = true)
    public List<UgcContentDto> list(ContentStatus status) {
        if (status == null) {
            return ugcContentRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
        }

        return ugcContentRepository.findTop100ByStatusOrderByCreatedAtDesc(status).stream().map(this::toDto).toList();
    }

    @Transactional
    public UgcContentDto publish(Long id) {
        UgcContent content = findById(id);
        content.setStatus(ContentStatus.PUBLISHED);
        return toDto(ugcContentRepository.save(content));
    }

    @Transactional
    public UgcContentDto archive(Long id) {
        UgcContent content = findById(id);
        content.setStatus(ContentStatus.ARCHIVED);
        return toDto(ugcContentRepository.save(content));
    }

    private UgcContent findById(Long id) {
        return ugcContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenu introuvable avec id " + id));
    }

    private UgcContentDto toDto(UgcContent content) {
        UgcContentDto dto = new UgcContentDto();
        dto.setId(content.getId());
        dto.setAuthorProfilId(content.getAuthorProfilId());
        dto.setTitle(content.getTitle());
        dto.setBody(content.getBody());
        dto.setStatus(content.getStatus());
        dto.setCreatedAt(content.getCreatedAt());
        return dto;
    }
}
