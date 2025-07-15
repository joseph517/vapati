package com.vaPaTi.vaPaTi.repository;
import com.vaPaTi.vaPaTi.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    // Search for bank accounts by user id
    @Query("SELECT ba FROM BankAccount ba WHERE ba.user.id = :userId")
    List<BankAccount> findByUserId(@Param("userId") Long userId);

    // Search for bank accounts by account number
    @Query("SELECT ba FROM BankAccount ba WHERE ba.accountNumber = :accountNumber")
    Optional<BankAccount> findByAccountNumber(@Param("accountNumber") String accountNumber);

    // Check if a bank account exists by account number
    @Query("SELECT COUNT(ba) > 0 FROM BankAccount ba WHERE ba.accountNumber = :accountNumber")
    boolean existsByAccountNumber(@Param("accountNumber") String accountNumber);

    // Check if a user is verified (for validations before creating an account)
    @Query("SELECT u.isVerified FROM User u WHERE u.id = :userId")
    Optional<Boolean> isUserVerified(@Param("userId") Long userId);

    //Checks if a bank account exists with the specified user ID and account number.
    @Query("SELECT COUNT(ba) > 0 FROM BankAccount ba WHERE ba.user.id = :userId AND ba.accountNumber = :accountNumber")
    boolean existsByUserIdAndAccountNumber(@Param("userId") Long userId, @Param("accountNumber") String accountNumber);

    boolean existsByAccountNumberAndDeletedAtIsNull(String accountNumber);

    boolean existsByUserIdAndAccountNumberAndDeletedAtIsNull(Long userId, String accountNumber);

    @Query(value = "SELECT * FROM bank_accounts WHERE user_id = :userId AND account_number = :accountNumber AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<BankAccount> findByUserIdAndAccountNumberAndDeletedAtIsNotNull(@Param("userId") Long userId, @Param("accountNumber") String accountNumber);

    @Query(value = "SELECT * FROM bank_accounts WHERE account_number = :accountNumber", nativeQuery = true)
    Optional<BankAccount> findByAccountNumberIgnoreDeleted(@Param("accountNumber") String accountNumber);
}
