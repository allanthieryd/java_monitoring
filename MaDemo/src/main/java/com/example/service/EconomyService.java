package com.example.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.WalletTransactionDto;
import com.example.dto.WalletTransactionRequest;
import com.example.entity.Profil;
import com.example.entity.TransactionType;
import com.example.entity.WalletTransaction;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.ProfilRepository;
import com.example.repository.WalletTransactionRepository;

import io.micrometer.core.annotation.Timed;

@Service
public class EconomyService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final ProfilRepository profilRepository;

    public EconomyService(WalletTransactionRepository walletTransactionRepository, ProfilRepository profilRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.profilRepository = profilRepository;
    }

    @Transactional
    @Timed(value = "service.economy.transaction")
    public WalletTransactionDto applyTransaction(WalletTransactionRequest request) {
        Profil profil = profilRepository.findById(request.getProfilId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable avec id " + request.getProfilId()));

        BigDecimal updatedCredits = profil.getCredits();
        if (request.getType() == TransactionType.CREDIT) {
            updatedCredits = updatedCredits.add(request.getAmount());
        } else {
            if (updatedCredits.compareTo(request.getAmount()) < 0) {
                throw new IllegalArgumentException("Credits insuffisants pour debiter ce montant");
            }
            updatedCredits = updatedCredits.subtract(request.getAmount());
        }

        profil.setCredits(updatedCredits);
        profilRepository.save(profil);

        WalletTransaction tx = new WalletTransaction();
        tx.setProfilId(request.getProfilId());
        tx.setType(request.getType());
        tx.setAmount(request.getAmount());
        tx.setReason(request.getReason());

        return toDto(walletTransactionRepository.save(tx));
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionDto> listWalletTransactions(Long profilId) {
        return walletTransactionRepository.findTop100ByProfilIdOrderByCreatedAtDesc(profilId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private WalletTransactionDto toDto(WalletTransaction tx) {
        WalletTransactionDto dto = new WalletTransactionDto();
        dto.setId(tx.getId());
        dto.setProfilId(tx.getProfilId());
        dto.setType(tx.getType());
        dto.setAmount(tx.getAmount());
        dto.setReason(tx.getReason());
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }
}
