package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    Boolean existsByName(String name);

    @Query("SELECT CASE WHEN COUNT(uc) > 0 THEN TRUE ELSE FALSE END " +
            "FROM UserCategory uc " +
            "WHERE uc.category.id = :categoryId")
    boolean isCategoryInUse(@Param("categoryId") Long categoryId);

}
