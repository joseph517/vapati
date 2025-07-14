package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
@Tag(name = "Bank Accounts", description = "Endpoints for Bank Accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    // Constructor
    public BankAccountController(
            BankAccountService bankAccountService
    ) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    @Operation(summary = "Create bank account", description = "Create a new bank account for a user")
    public ResponseEntity<BankAccountDTO> createBankAccount(@RequestBody CreateBankAccountDTO dto) {
        return ResponseEntity.ok(bankAccountService.createBankAccount(dto));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all user bank accounts", description = "Get all bank accounts by user id")
    public ResponseEntity<List<BankAccountDTO>> getBankAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(bankAccountService.getBankAccountsByUserId(userId));
    }
}
