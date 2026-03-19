package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.WalletTransactionDto;
import com.example.dto.WalletTransactionRequest;
import com.example.service.EconomyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/economy")
public class EconomyController {

    private final EconomyService economyService;

    public EconomyController(EconomyService economyService) {
        this.economyService = economyService;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletTransactionDto createTransaction(@Valid @RequestBody WalletTransactionRequest request) {
        return economyService.applyTransaction(request);
    }

    @GetMapping("/wallet/{profilId}/transactions")
    public List<WalletTransactionDto> listWalletTransactions(@PathVariable Long profilId) {
        return economyService.listWalletTransactions(profilId);
    }
}
