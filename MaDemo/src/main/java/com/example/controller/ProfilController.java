package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.dto.ProfilDto;
import com.example.service.ProfilService;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profils")
@Validated
public class ProfilController {

    private final ProfilService profilService;

    public ProfilController(ProfilService profilService) {
        this.profilService = profilService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Timed(value = "api.profil.create", description = "Temps de creation d'un profil")
    public ProfilDto create(@Valid @RequestBody ProfilDto profilDto) {
        return profilService.createProfil(profilDto);
    }

    @GetMapping("/{id}")
    @Timed(value = "api.profil.get", description = "Temps de lecture d'un profil")
    public ProfilDto getById(@PathVariable Long id) {
        return profilService.getProfil(id);
    }

    @GetMapping
    @Timed(value = "api.profil.list", description = "Temps de lecture de la liste des profils")
    public List<ProfilDto> list() {
        return profilService.listProfils();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Timed(value = "api.profil.delete", description = "Temps de suppression d'un profil")
    public void delete(@PathVariable Long id) {
        profilService.deleteProfil(id);
    }
}

