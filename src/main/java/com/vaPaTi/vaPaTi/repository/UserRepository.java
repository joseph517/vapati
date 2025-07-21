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

}
