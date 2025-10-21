package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.BankAccount;
import com.vaPaTi.vaPaTi.entity.Role;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("docker")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BankAccountRepositoryTest {

    @Autowired
    private BankAccountRepository bankAccountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testFindBankAccountByAccountNumber() {
        // Arrange
        String accountNumber = "1234567890";

        User user = User.builder()
                .active(true)
                .verified(true)
                .build();

        Role role = Role.builder()
                .name("ROLE_USER")
                .build();
        role = roleRepository.save(role);
        user.setRole(role);

        user.setUserInfo(UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .email("ZV4aD@example.com")
                .userName("john.doe")
                .password("securePassword123")
                .phone("1234567890")
                .description("Test user")
                .build());

        user = userRepository.save(user);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        bankAccount.setBankName("Banco de Prueba");
        bankAccount.setAccountNumber(accountNumber);
        bankAccount.setAccountType("Corriente");
        bankAccount.setAccountHolder("John Doe");
        bankAccount.setVerified(true);

        bankAccountRepository.save(bankAccount);

        // Act
        Optional<BankAccount> foundBankAccount = bankAccountRepository.findByAccountNumber(accountNumber);

        // Assert
        assertTrue(foundBankAccount.isPresent());
        assertEquals(accountNumber, foundBankAccount.get().getAccountNumber());
    }

    @Test
    void testFindBankAccountByAccountNumber_NotFound() {
        // Arrange
        String nonExistentAccountNumber = "9999999999";

        // Act
        Optional<BankAccount> foundBankAccount = bankAccountRepository.findByAccountNumber(nonExistentAccountNumber);

        // Assert
        assertTrue(foundBankAccount.isEmpty());
    }

    @Test
    void testFindBankAccountByAccountNumber_NullParameter() {
        // Act & Assert
        Optional<BankAccount> foundBankAccount = bankAccountRepository.findByAccountNumber(null);
        assertTrue(foundBankAccount.isEmpty());
    }

    @Test
    void testFindBankAccountByAccountNumber_EmptyString() {
        // Act
        Optional<BankAccount> foundBankAccount = bankAccountRepository.findByAccountNumber("");

        // Assert
        assertTrue(foundBankAccount.isEmpty());
    }

    @Test
    void testFindByUserId_UserWithMultipleAccounts() {
        // Arrange
        User user = createTestUser("john.doe.multiple", "john.multiple@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();

        BankAccount account1 = createTestBankAccount(user, "1111111111", "Cuenta Corriente");
        BankAccount account2 = createTestBankAccount(user, "2222222222", "Cuenta Ahorros");
        BankAccount account3 = createTestBankAccount(user, "3333333333", "Cuenta Nómina");

        bankAccountRepository.save(account1);
        bankAccountRepository.save(account2);
        bankAccountRepository.save(account3);

        // Act
        List<BankAccount> foundAccounts = bankAccountRepository.findByUserId(userId);

        // Assert
        assertNotNull(foundAccounts);
        assertEquals(3, foundAccounts.size());

        foundAccounts.forEach(account ->
                assertEquals(userId, account.getUser().getId())
        );

        List<String> accountNumbers = foundAccounts.stream()
                .map(BankAccount::getAccountNumber)
                .toList();

        assertTrue(accountNumbers.contains("1111111111"));
        assertTrue(accountNumbers.contains("2222222222"));
        assertTrue(accountNumbers.contains("3333333333"));
    }

    @Test
    void testFindByUserId_UserWithSingleAccount() {
        // Arrange
        User user = createTestUser("john.doe.single", "john.single@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();

        BankAccount account = createTestBankAccount(user, "4444444444", "Cuenta Corriente");
        bankAccountRepository.save(account);

        // Act
        List<BankAccount> foundAccounts = bankAccountRepository.findByUserId(userId);

        // Assert
        assertNotNull(foundAccounts);
        assertEquals(1, foundAccounts.size());

        BankAccount foundAccount = foundAccounts.get(0);
        assertEquals(userId, foundAccount.getUser().getId());
        assertEquals("4444444444", foundAccount.getAccountNumber());
        assertEquals("Cuenta Corriente", foundAccount.getAccountType());
        assertEquals("Banco de Prueba", foundAccount.getBankName());
    }

    @Test
    void testFindByUserId_UserWithNoAccounts() {
        // Arrange
        User user = createTestUser("john.doe.noaccounts", "john.noaccounts@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();

        // Act
        List<BankAccount> foundAccounts = bankAccountRepository.findByUserId(userId);

        // Assert
        assertNotNull(foundAccounts);
        assertTrue(foundAccounts.isEmpty());
        assertEquals(0, foundAccounts.size());
    }

    @Test
    void testFindByUserId_NonExistentUser() {
        // Arrange
        Long nonExistentUserId = 99999L;

        // Act
        List<BankAccount> foundAccounts = bankAccountRepository.findByUserId(nonExistentUserId);

        // Assert
        assertNotNull(foundAccounts);
        assertTrue(foundAccounts.isEmpty());
        assertEquals(0, foundAccounts.size());
    }

    @Test
    void testFindByUserId_NullUserId() {
        // Arrange
        Long nullUserId = null;

        // Act
        List<BankAccount> foundAccounts = bankAccountRepository.findByUserId(nullUserId);

        // Assert
        assertNotNull(foundAccounts);
        assertTrue(foundAccounts.isEmpty());
    }

    @Test
    void testExistsByAccountNumber_AccountExists() {
        // Arrange
        String existingAccountNumber = "7777777777";

        User user = createTestUser("john.exists", "john.exists@example.com");
        user = userRepository.save(user);

        BankAccount account = createTestBankAccount(user, existingAccountNumber, "Test Account");
        bankAccountRepository.save(account);

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumber(existingAccountNumber);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByAccountNumber_AccountDoesNotExist() {
        // Arrange
        String nonExistentAccountNumber = "9999999999";

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumber(nonExistentAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByAccountNumber_NullAccountNumber() {
        // Arrange
        String nullAccountNumber = null;

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumber(nullAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByAccountNumber_EmptyAccountNumber() {
        // Arrange
        String emptyAccountNumber = "";

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumber(emptyAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByAccountNumber_WithDeletedAccount() {
        // Arrange
        String accountNumber = "8888888888";

        User user = createTestUser("john.deleted.exists", "john.deleted.exists@example.com");
        user = userRepository.save(user);

        BankAccount deletedAccount = createTestBankAccount(user, accountNumber, "Deleted Account");
        // deletedAccount.setDeletedAt(LocalDateTime.now()); // If you have soft delete
        bankAccountRepository.save(deletedAccount);

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumber(accountNumber);

        // Assert
        assertTrue(exists); // Should include deleted accounts
    }

    @Test
    void testIsUserVerified_UserVerifiedTrue() {
        // Arrange
        User verifiedUser = createTestUser("verified.user", "verified@example.com");
        verifiedUser.setVerified(true);
        verifiedUser = userRepository.save(verifiedUser);

        final Long userId = verifiedUser.getId();

        // Act
        Optional<Boolean> isVerified = bankAccountRepository.isUserVerified(userId);

        // Assert
        assertTrue(isVerified.isPresent());
        assertTrue(isVerified.get());
    }

    @Test
    void testIsUserVerified_UserVerifiedFalse() {
        // Arrange
        User unverifiedUser = createTestUser("unverified.user", "unverified@example.com");
        unverifiedUser.setVerified(false);
        unverifiedUser = userRepository.save(unverifiedUser);

        final Long userId = unverifiedUser.getId();

        // Act
        Optional<Boolean> isVerified = bankAccountRepository.isUserVerified(userId);

        // Assert
        assertTrue(isVerified.isPresent());
        assertFalse(isVerified.get());
    }

    @Test
    void testIsUserVerified_UserDoesNotExist() {
        // Arrange
        Long nonExistentUserId = 99999L;

        // Act
        Optional<Boolean> isVerified = bankAccountRepository.isUserVerified(nonExistentUserId);

        // Assert
        assertTrue(isVerified.isEmpty());
    }

    @Test
    void testIsUserVerified_NullUserId() {
        // Arrange
        Long nullUserId = null;

        // Act
        Optional<Boolean> isVerified = bankAccountRepository.isUserVerified(nullUserId);

        // Assert
        assertTrue(isVerified.isEmpty());
    }

    @Test
    void testExistsByUserIdAndAccountNumber_CombinationExists() {
        // Arrange
        User user = createTestUser("combination.user", "combination@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();
        String accountNumber = "1010101010";

        BankAccount account = createTestBankAccount(user, accountNumber, "Test Account");
        bankAccountRepository.save(account);

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumber(userId, accountNumber);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumber_UserExistsAccountNumberDoesNot() {
        // Arrange
        User user = createTestUser("user.exists", "user.exists@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();
        String existingAccountNumber = "2020202020";
        String nonExistingAccountNumber = "3030303030";

        BankAccount account = createTestBankAccount(user, existingAccountNumber, "Test Account");
        bankAccountRepository.save(account);

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumber(userId, nonExistingAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumber_AccountExistsUserDoesNot() {
        // Arrange
        User user1 = createTestUser("user.one", "user.one@example.com");
        user1 = userRepository.save(user1);

        User user2 = createTestUser("user.two", "user.two@example.com");
        user2 = userRepository.save(user2);

        final Long user2Id = user2.getId();
        String accountNumber = "4040404040";

        // Create account for user1
        BankAccount account = createTestBankAccount(user1, accountNumber, "Test Account");
        bankAccountRepository.save(account);

        // Act - Check if user2 has this account number
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumber(user2Id, accountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumber_NeitherExists() {
        // Arrange
        Long nonExistentUserId = 99999L;
        String nonExistentAccountNumber = "9999999999";

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumber(nonExistentUserId, nonExistentAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumber_NullUserId() {
        // Arrange
        Long nullUserId = null;
        String accountNumber = "5050505050";

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumber(nullUserId, accountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumber_NullAccountNumber() {
        // Arrange
        User user = createTestUser("null.account", "null.account@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();
        String nullAccountNumber = null;

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumber(userId, nullAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumber_BothParametersNull() {
        // Arrange
        Long nullUserId = null;
        String nullAccountNumber = null;

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumber(nullUserId, nullAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumber_WithDeletedAccount() {
        // Arrange
        User user = createTestUser("deleted.combo", "deleted.combo@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();
        String accountNumber = "6060606060";

        BankAccount deletedAccount = createTestBankAccount(user, accountNumber, "Deleted Account");
        // deletedAccount.setDeletedAt(LocalDateTime.now()); // If you have soft delete
        bankAccountRepository.save(deletedAccount);

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumber(userId, accountNumber);

        // Assert
        assertTrue(exists); // Should include deleted accounts (since this method doesn't filter by deletedAt)
    }

    @Test
    void testExistsByAccountNumberAndDeletedAtIsNull_AccountExistsNotDeleted() {
        // Arrange
        String accountNumber = "7070707070";

        User user = createTestUser("active.account", "active.account@example.com");
        user = userRepository.save(user);

        BankAccount activeAccount = createTestBankAccount(user, accountNumber, "Active Account");
        // Ensure deletedAt is null (default behavior)
        bankAccountRepository.save(activeAccount);

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(accountNumber);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByAccountNumberAndDeletedAtIsNull_AccountExistsButDeleted() {
        // Arrange
        String accountNumber = "8080808080";

        User user = createTestUser("deleted.account", "deleted.account@example.com");
        user = userRepository.save(user);

        BankAccount deletedAccount = createTestBankAccount(user, accountNumber, "Deleted Account");
        // Set deletedAt to mark as deleted
        deletedAccount.setDeletedAt(LocalDateTime.now());
        bankAccountRepository.save(deletedAccount);

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(accountNumber);

        // Assert
        assertFalse(exists); // Should not find deleted accounts
    }

    @Test
    void testExistsByAccountNumberAndDeletedAtIsNull_AccountDoesNotExist() {
        // Arrange
        String nonExistentAccountNumber = "9090909090";

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(nonExistentAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByAccountNumberAndDeletedAtIsNull_NullAccountNumber() {
        // Arrange
        String nullAccountNumber = null;

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(nullAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByAccountNumberAndDeletedAtIsNull_EmptyAccountNumber() {
        // Arrange
        String emptyAccountNumber = "";

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(emptyAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByAccountNumberAndDeletedAtIsNull_MixedActiveAndDeleted() {
        // Arrange
        String accountNumber = "1111222233";

        User user1 = createTestUser("user.active", "user.active@example.com");
        user1 = userRepository.save(user1);

        User user2 = createTestUser("user.deleted", "user.deleted@example.com");
        user2 = userRepository.save(user2);

        // Create active account
        BankAccount activeAccount = createTestBankAccount(user1, accountNumber, "Active Account");
        bankAccountRepository.save(activeAccount);

        // Create deleted account with same account number (if your business logic allows this)
        BankAccount deletedAccount = createTestBankAccount(user2, accountNumber + "DEL", "Deleted Account");
        deletedAccount.setDeletedAt(LocalDateTime.now());
        bankAccountRepository.save(deletedAccount);

        // Act
        boolean exists = bankAccountRepository.existsByAccountNumberAndDeletedAtIsNull(accountNumber);

        // Assert
        assertTrue(exists); // Should find the active one
    }

    @Test
    void testExistsByUserIdAndAccountNumberAndDeletedAtIsNull_CombinationExistsNotDeleted() {
        // Arrange
        User user = createTestUser("combo.active", "combo.active@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();
        String accountNumber = "1122334455";

        BankAccount activeAccount = createTestBankAccount(user, accountNumber, "Active Account");
        bankAccountRepository.save(activeAccount);

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, accountNumber);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumberAndDeletedAtIsNull_CombinationExistsButDeleted() {
        // Arrange
        User user = createTestUser("combo.deleted", "combo.deleted@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();
        String accountNumber = "2233445566";

        BankAccount deletedAccount = createTestBankAccount(user, accountNumber, "Deleted Account");
        deletedAccount.setDeletedAt(LocalDateTime.now());
        bankAccountRepository.save(deletedAccount);

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, accountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumberAndDeletedAtIsNull_UserExistsAccountDoesNot() {
        // Arrange
        User user = createTestUser("user.no.account", "user.no.account@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();
        String nonExistentAccountNumber = "9999888877";

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, nonExistentAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumberAndDeletedAtIsNull_AccountExistsDifferentUser() {
        // Arrange
        User user1 = createTestUser("owner.user", "owner.user@example.com");
        user1 = userRepository.save(user1);

        User user2 = createTestUser("other.user", "other.user@example.com");
        user2 = userRepository.save(user2);

        final Long user2Id = user2.getId();
        String accountNumber = "3344556677";

        BankAccount account = createTestBankAccount(user1, accountNumber, "User1 Account");
        bankAccountRepository.save(account);

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(user2Id, accountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumberAndDeletedAtIsNull_BothNull() {
        // Arrange
        Long nullUserId = null;
        String nullAccountNumber = null;

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(nullUserId, nullAccountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumberAndDeletedAtIsNull_NullUserIdValidAccount() {
        // Arrange
        Long nullUserId = null;
        String accountNumber = "4455667788";

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(nullUserId, accountNumber);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUserIdAndAccountNumberAndDeletedAtIsNull_ValidUserIdNullAccount() {
        // Arrange
        User user = createTestUser("valid.user.null.account", "valid.null@example.com");
        user = userRepository.save(user);

        final Long userId = user.getId();
        String nullAccountNumber = null;

        // Act
        boolean exists = bankAccountRepository.existsByUserIdAndAccountNumberAndDeletedAtIsNull(userId, nullAccountNumber);

        // Assert
        assertFalse(exists);
    }

    private User createTestUser(String username, String email) {
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_USER")
                        .build()));

        User user = User.builder()
                .active(true)
                .verified(true)
                .role(role)
                .build();

        user.setUserInfo(UserInfo.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .userName(username)
                .password("securePassword123" + UUID.randomUUID())
                .phone("12345" + UUID.randomUUID().toString().substring(0, 5))
                .description("Test user")
                .build());

        return userRepository.save(user);
    }
    private BankAccount createTestBankAccount(User user, String accountNumber, String accountType) {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        bankAccount.setBankName("Banco de Prueba");
        bankAccount.setAccountNumber(accountNumber);
        bankAccount.setAccountType(accountType);
        bankAccount.setAccountHolder("John Doe");
        bankAccount.setVerified(true);

        return bankAccount;
    }

}

