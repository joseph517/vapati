package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @NotNull Optional<User> findById(@NotNull Long id);

    // Used by the JWT filter on every request: one SELECT. userInfo and verificationRequest are inverse @OneToOne,
    // Hibernate would load them with separate SELECTs otherwise. @SQLRestriction still hides deleted accounts
    @EntityGraph(attributePaths = {"userInfo", "role", "verificationRequest"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForRequest(@Param("id") Long id);

    // One SELECT. @SQLRestriction hides deleted accounts, so a deleted admin gives false
    boolean existsByIdAndRole_Name(Long id, String roleName);

    @EntityGraph(attributePaths = {
            "userInfo",
            "userCategories",
            "userCategories.category",
            "bankAccounts",
    })
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithFullDetails(@Param("id") Long id);


    @EntityGraph(
            attributePaths = {
                    "userInfo",
                    "userCategories",
                    "userCategories.category"
            }
    )
    @Query("SELECT DISTINCT u FROM User u")
    List<User> findAllWithDetails();

    // Version with pagination
    @EntityGraph(
            attributePaths = {
                    "userInfo",
                    "userCategories",
                    "userCategories.category"
            }
    )
    @Query("SELECT DISTINCT u FROM User u")
    Page<User> findAllWithDetails(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NOT NULL")
    Optional<User> findDeletedById(@Param("id") Long id);

    @Query(value = "SELECT u.* FROM [user] u JOIN user_info ui ON u.id = ui.user_id WHERE ui.email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    // Native so @SQLRestriction does not hide soft-deleted accounts: moderation must reach them too
    @Query(value = "SELECT u.* FROM [user] u WHERE u.id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") Long id);

}
