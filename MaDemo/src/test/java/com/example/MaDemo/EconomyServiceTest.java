package com.example.MaDemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.dto.WalletTransactionDto;
import com.example.dto.WalletTransactionRequest;
import com.example.entity.Profil;
import com.example.entity.TransactionType;
import com.example.entity.WalletTransaction;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.ProfilRepository;
import com.example.repository.WalletTransactionRepository;
import com.example.service.EconomyService;

@ExtendWith(MockitoExtension.class)
class EconomyServiceTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private ProfilRepository profilRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EconomyService economyService;

    @BeforeEach
    void setUp() {
        economyService = new EconomyService(walletTransactionRepository, profilRepository, eventPublisher);
    }

    @Test
    void credit_shouldIncreaseBalance() {
        Profil profil = profil(1L, BigDecimal.valueOf(100));
        when(profilRepository.findById(1L)).thenReturn(Optional.of(profil));
        when(profilRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletTransaction tx = savedTx(1L, TransactionType.CREDIT, BigDecimal.valueOf(50));
        when(walletTransactionRepository.save(any())).thenReturn(tx);

        WalletTransactionDto result = economyService.applyTransaction(creditRequest(1L, BigDecimal.valueOf(50)));

        assertThat(profil.getCredits()).isEqualByComparingTo("150");
        assertThat(result.getType()).isEqualTo(TransactionType.CREDIT);
    }

    @Test
    void debit_shouldDecreaseBalance() {
        Profil profil = profil(1L, BigDecimal.valueOf(200));
        when(profilRepository.findById(1L)).thenReturn(Optional.of(profil));
        when(profilRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletTransaction tx = savedTx(1L, TransactionType.DEBIT, BigDecimal.valueOf(75));
        when(walletTransactionRepository.save(any())).thenReturn(tx);

        WalletTransactionRequest req = new WalletTransactionRequest();
        req.setProfilId(1L);
        req.setType(TransactionType.DEBIT);
        req.setAmount(BigDecimal.valueOf(75));
        req.setReason("achat");

        economyService.applyTransaction(req);

        assertThat(profil.getCredits()).isEqualByComparingTo("125");
    }

    @Test
    void debit_shouldThrow_whenInsufficientFunds() {
        Profil profil = profil(1L, BigDecimal.valueOf(10));
        when(profilRepository.findById(1L)).thenReturn(Optional.of(profil));

        WalletTransactionRequest req = new WalletTransactionRequest();
        req.setProfilId(1L);
        req.setType(TransactionType.DEBIT);
        req.setAmount(BigDecimal.valueOf(50));
        req.setReason("achat");

        assertThatThrownBy(() -> economyService.applyTransaction(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("insuffisants");
    }

    @Test
    void applyTransaction_shouldThrow_whenProfilNotFound() {
        when(profilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> economyService.applyTransaction(creditRequest(99L, BigDecimal.TEN)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- helpers ---

    private Profil profil(Long id, BigDecimal credits) {
        Profil p = new Profil();
        p.setId(id);
        p.setName("Joueur");
        p.setEmail("joueur@test.fr");
        p.setCredits(credits);
        return p;
    }

    private WalletTransactionRequest creditRequest(Long profilId, BigDecimal amount) {
        WalletTransactionRequest req = new WalletTransactionRequest();
        req.setProfilId(profilId);
        req.setType(TransactionType.CREDIT);
        req.setAmount(amount);
        req.setReason("bonus");
        return req;
    }

    private WalletTransaction savedTx(Long id, TransactionType type, BigDecimal amount) {
        WalletTransaction tx = new WalletTransaction();
        tx.setId(id);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setReason("test");
        return tx;
    }
}
