package com.example.event;

import com.example.entity.TransactionType;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * Publié après chaque transaction sur le portefeuille d'un joueur.
 * Utile pour la traçabilité financière et les alertes
 * (ex: transactions inhabituellement élevées).
 */
public class WalletTransactionEvent extends ApplicationEvent {

    private final Long transactionId;
    private final Long profilId;
    private final TransactionType type;
    private final BigDecimal amount;

    public WalletTransactionEvent(Object source, Long transactionId, Long profilId,
                                  TransactionType type, BigDecimal amount) {
        super(source);
        this.transactionId = transactionId;
        this.profilId = profilId;
        this.type = type;
        this.amount = amount;
    }

    public Long getTransactionId() { return transactionId; }
    public Long getProfilId() { return profilId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
}
