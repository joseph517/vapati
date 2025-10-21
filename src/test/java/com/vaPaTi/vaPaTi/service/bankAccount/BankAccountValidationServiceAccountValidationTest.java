package com.vaPaTi.vaPaTi.service.bankAccount;

public class BankAccountValidationServiceAccountValidationTest {

//    public AccountValidationResult validateAccountCreation(String accountNumber, Long userId) {
//        Optional<BankAccount> existingAccount = bankAccountRepository.findByAccountNumberIgnoreDeleted(accountNumber);
//
//        if (existingAccount.isEmpty()) {
//            return AccountValidationResult.CAN_CREATE;
//        }
//
//        BankAccount account = existingAccount.get();
//        boolean isActive = account.getDeletedAt() == null;
//        boolean isOwner = account.getUser().getId().equals(userId);
//
//        if (isActive) {
//            return AccountValidationResult.ALREADY_EXISTS;
//        }
//
//        if (!isOwner) {
//            return AccountValidationResult.OWNED_BY_OTHER_USER;
//        }
//
//        return AccountValidationResult.CAN_RESTORE;
//    }
//
//    public void checkIfUserHasDuplicateAccount(Long userId, String accountNumber) {
//        if (bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, accountNumber)) {
//            throw new MessageException("User already has a bank account with this account number");
//        }
//    }
//
//    public void validateAccountNumberForUpdate(String newAccountNumber, String currentAccountNumber, Long userId) {
//        if (newAccountNumber == null || newAccountNumber.trim().isEmpty()) {
//            throw new MessageException("Account number cannot be empty");
//        }
//
//        String trimmedNewAccountNumber = newAccountNumber.trim();
//
//        if (trimmedNewAccountNumber.equals(currentAccountNumber)) {
//            return;
//        }
//
//        if (bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(trimmedNewAccountNumber)) {
//            throw new MessageException("Account number already exists");
//        }
//
//        Optional<BankAccount> existingAccount = bankAccountRepository.findByAccountNumberIgnoreDeleted(trimmedNewAccountNumber);
//        if (existingAccount.isPresent()) {
//            BankAccount existing = existingAccount.get();
//            if (!existing.getUser().getId().equals(userId)) {
//                throw new MessageException("Cannot use account number owned by another user");
//            }
//        }
//    }

}
