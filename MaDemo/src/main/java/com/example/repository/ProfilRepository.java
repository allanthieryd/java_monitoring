package com.example.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Profil;

public interface ProfilRepository extends JpaRepository<Profil, Long> {

    Optional<Profil> findByEmail(String email);

    List<Profil> findTop20ByOrderByRankPointsDesc();
}