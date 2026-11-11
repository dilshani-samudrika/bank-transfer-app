package com.example.bankapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Saves a FAILED record after a transfer was rolled back.
 * REQUIRES_NEW = always start a brand-new, independent transaction,
 * so this record is committed even though the transfer itself was rolled back.
 */
@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private final BankTransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BankTransaction recordFailedTransaction(
            String fromAccount,
            String toAccount,
            BigDecimal amount) {

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setTimestamp(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }
}
