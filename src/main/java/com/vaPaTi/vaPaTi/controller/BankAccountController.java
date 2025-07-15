package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.BankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.CreateBankAccountDTO;
import com.vaPaTi.vaPaTi.dtos.UpdateBankAccountDTO;
import com.vaPaTi.vaPaTi.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @Operation(summary = "Get bank accounts by user ID", description = "Get a list of bank accounts for a specific user")
    public ResponseEntity<List<BankAccountDTO>> getBankAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(bankAccountService.getBankAccountsByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete bank account", description = "Delete bank account by ID")
    public ResponseEntity<Map<String, String>> deleteBankAccount(@PathVariable Long id) {
        bankAccountService.deleteBankAccount(id);
        return ResponseEntity.ok(Map.of(
                "message", "Bank account deleted successfully",
                "success", "true"
        ));
    }


    // TODO: implement user validation by auth token
    @PutMapping("{id}")
    @Operation(summary = "Update bank account", description = "Update bank account by ID")
    public ResponseEntity<BankAccountDTO> updateBankAccount(
            @PathVariable Long id,
            @RequestBody @Valid UpdateBankAccountDTO dto) {

        BankAccountDTO updated = bankAccountService.updateBankAccount(id, dto);
        return ResponseEntity.ok(updated);
    }

}
