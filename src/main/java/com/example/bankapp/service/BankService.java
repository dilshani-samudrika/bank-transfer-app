
package com.example.bankapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.exception.SameAccountException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    @Transactional
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Task 10: Temporarily disable transaction protection
    @Transactional(rollbackFor = Exception.class)
    //@Transactional(propagation = Propagation.SUPPORTS)
    public BankTransaction transfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            boolean simulateFailure) {

        if (fromAccount.equals(toAccount)) {
            throw new SameAccountException(
                    "Cannot transfer money to the same account");
        }

        Customer sender = customerRepository.getByAccountNumber(fromAccount)
                .orElseThrow(() ->
                        new AccountNotFoundException("Sender account not found"));

        customerRepository.getByAccountNumber(toAccount)
                .orElseThrow(() ->
                        new AccountNotFoundException("Receiver account not found"));

        int debited = customerRepository.debit(fromAccount, amount);

        if (debited == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        if (simulateFailure) {
            throw new RuntimeException(
                    "Simulated system failure");
        }

        int credited = customerRepository.credit(toAccount, amount);

        if (credited == 0) {
            throw new AccountNotFoundException(
                    "Receiver account not found");
        }

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setTimestamp(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }
}
