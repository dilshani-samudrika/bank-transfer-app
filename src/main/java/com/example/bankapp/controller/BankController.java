package com.example.bankapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;
    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // Dashboard
    @GetMapping("/")
    public String dashboard(
            @org.springframework.web.bind.annotation.RequestParam(
                    required = false) String keyword,
            Model model) {

        if (keyword == null || keyword.isBlank()) {
            model.addAttribute("customers",
                    customerRepository.findAllOrdered());
        } else {
            model.addAttribute("customers",
                    customerRepository.searchByName(keyword));
        }

        model.addAttribute("allCustomers",
                customerRepository.findAllOrdered());
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalBalance",
                customerRepository.getTotalBalance());
        model.addAttribute("totalCustomers",
                customerRepository.countCustomersNative());
        model.addAttribute("successCount",
                transactionRepository.countByStatus(
                        TransactionStatus.SUCCESS));
        model.addAttribute("failedCount",
                transactionRepository.countByStatus(
                        TransactionStatus.FAILED));
        model.addAttribute("transactions",
                transactionRepository.findAllLatest());

        model.addAttribute("customer", new Customer());
        model.addAttribute("transferRequest", new TransferRequest());

        return "bank";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", customerRepository.findAllOrdered());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        if (accountNumber.isBlank() || amount.signum() <= 0) {
            redirectAttributes.addFlashAttribute(
                    "error", "Select an account and enter a positive amount.");
            return "redirect:/deposit";
        }

        if (customerRepository.credit(accountNumber, amount) == 0) {
            redirectAttributes.addFlashAttribute(
                    "error", "Customer account was not found.");
            return "redirect:/deposit";
        }

        redirectAttributes.addFlashAttribute(
                "success", "Money deposited successfully.");
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", customerRepository.findAllOrdered());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        if (accountNumber.isBlank() || amount.signum() <= 0) {
            redirectAttributes.addFlashAttribute(
                    "error", "Select an account and enter a positive amount.");
            return "redirect:/withdraw";
        }

        if (customerRepository.getByAccountNumber(accountNumber).isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error", "Customer account was not found.");
            return "redirect:/withdraw";
        }

        if (customerRepository.debit(accountNumber, amount) == 0) {
            redirectAttributes.addFlashAttribute(
                    "error", "Insufficient balance.");
            return "redirect:/withdraw";
        }

        redirectAttributes.addFlashAttribute(
                "success", "Money withdrawn successfully.");
        return "redirect:/withdraw";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers",
                transactionRepository
                        .findByStatusAndAmountGreaterThanOrderByTimestampDesc(
                                TransactionStatus.SUCCESS, min));
        return "large-transfers";
    }

    // Add a customer
    @PostMapping("/customers")
    public String addCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error", "Please enter valid customer details.");
            return "redirect:/";
        }

        if (customerRepository.countAccounts(
                customer.getAccountNumber()) > 0) {
            redirectAttributes.addFlashAttribute(
                    "error", "Account number already exists.");
            return "redirect:/";
        }

        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            redirectAttributes.addFlashAttribute(
                    "error", "Email already exists.");
            return "redirect:/";
        }

        bankService.createCustomer(customer);

        redirectAttributes.addFlashAttribute(
                "success", "Customer added successfully.");

        return "redirect:/";
    }

    // Transfer money
    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest")
            TransferRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error", "Please enter valid transfer details.");
            return "redirect:/";
        }

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure());

            redirectAttributes.addFlashAttribute(
                    "success", "Transfer completed successfully.");

        } catch (Exception e) {

            auditService.recordFailedTransaction(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount());

            if (request.isSimulateFailure()) {
                redirectAttributes.addFlashAttribute(
                        "error", "All changes were rolled back.");
            } else {
                redirectAttributes.addFlashAttribute(
                        "error", e.getMessage());
            }
        }

        return "redirect:/";
    }

    // View account transaction history
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model,
            RedirectAttributes redirectAttributes) {

        Customer customer = customerRepository
                .getByAccountNumber(accountNumber)
                .orElse(null);
        if (customer == null) {
            redirectAttributes.addFlashAttribute(
                    "error", "Customer account was not found.");
            return "redirect:/";
        }

        model.addAttribute("customer", customer);
        model.addAttribute("totalSent",
                transactionRepository.sumOutgoingByAccountAndStatus(
                        accountNumber, TransactionStatus.SUCCESS));
        model.addAttribute("transactions",
                transactionRepository.findHistory(accountNumber));

        return "history";
    }
}
