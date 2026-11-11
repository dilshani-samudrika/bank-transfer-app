package com.example.bankapp.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{6,20}",message = "Account number must contain 6 to 20 digits")
    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @NotBlank(message = "Customer name is required")
    @Size(max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 120)
    @Column(unique = true, nullable = false, length = 120)
    private String email;

    @NotNull
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Digits(integer = 12, fraction = 2)
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal balance;
}
