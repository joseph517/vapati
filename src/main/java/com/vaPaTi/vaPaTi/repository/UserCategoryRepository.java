package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.UserCategory;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {
    List<UserCategory> findByUser(User user);
    boolean existsByUserAndCategory(User user, Category category);
    long countByUser(User user);
}
