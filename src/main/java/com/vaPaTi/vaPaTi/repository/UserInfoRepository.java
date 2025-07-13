package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    boolean existsByEmailAndUserIdNot(String email, Long userId);
    boolean existsByUserNameAndUserIdNot(String userName, Long userId);
}
