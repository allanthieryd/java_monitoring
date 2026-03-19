package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Profil;

public interface ProfilRepository extends JpaRepository<Profil, Long> {
}