package com.example.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.ProfilDto;
import com.example.entity.Profil;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.ProfilRepository;
import com.example.util.DtoEntityUtil;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class ProfilService {

    private final ProfilRepository profilRepository;
    private final Counter profilCreationCounter;

    public ProfilService(ProfilRepository profilRepository, MeterRegistry meterRegistry) {
        this.profilRepository = profilRepository;
        this.profilCreationCounter = Counter.builder("profil_created_total")
                .description("Nombre total de profils crees")
                .register(meterRegistry);
    }

    @Transactional
    public ProfilDto createProfil(ProfilDto profilDto) {
        Profil profil = DtoEntityUtil.toEntity(profilDto);
        Profil saved = profilRepository.save(profil);
        profilCreationCounter.increment();
        return DtoEntityUtil.toDto(saved);
    }

    @Transactional(readOnly = true)
    public ProfilDto getProfil(Long id) {
        Profil profil = profilRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable avec id " + id));
        return DtoEntityUtil.toDto(profil);
    }

    @Transactional(readOnly = true)
    public List<ProfilDto> listProfils() {
        return profilRepository.findAll()
                .stream()
                .map(DtoEntityUtil::toDto)
                .toList();
    }

    @Transactional
    public void deleteProfil(Long id) {
        if (!profilRepository.existsById(id)) {
            throw new ResourceNotFoundException("Profil introuvable avec id " + id);
        }
        profilRepository.deleteById(id);
    }
}

