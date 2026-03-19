package com.example.MaDemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dto.ProfilDto;
import com.example.entity.Profil;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.ProfilRepository;
import com.example.service.ProfilService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class ProfilServiceTest {

    @Mock
    private ProfilRepository profilRepository;

    private ProfilService profilService;

    @BeforeEach
    void setUp() {
        profilService = new ProfilService(profilRepository, new SimpleMeterRegistry());
    }

    @Test
    void createProfil_shouldReturnSavedDto() {
        Profil savedProfil = new Profil();
        savedProfil.setId(1L);
        savedProfil.setName("Test Profil");

        when(profilRepository.save(any(Profil.class))).thenReturn(savedProfil);

        ProfilDto dto = new ProfilDto();
        dto.setName("Test Profil");

        ProfilDto result = profilService.createProfil(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Profil");
    }

    @Test
    void getProfil_shouldThrowWhenIdNotFound() {
        when(profilRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profilService.getProfil(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void deleteProfil_shouldDeleteWhenExists() {
        when(profilRepository.existsById(3L)).thenReturn(true);

        profilService.deleteProfil(3L);

        verify(profilRepository).deleteById(eq(3L));
    }

    @Test
    void deleteProfil_shouldThrowWhenMissing() {
        when(profilRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> profilService.deleteProfil(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(profilRepository, never()).deleteById(any(Long.class));
    }
}
