package com.vaPaTi.vaPaTi.repository.user;

import com.vaPaTi.vaPaTi.entity.user.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    // Same as UserSocialLinkRepository.findByUserId
    List<UserTag> findByUserId(Long userId);
}
