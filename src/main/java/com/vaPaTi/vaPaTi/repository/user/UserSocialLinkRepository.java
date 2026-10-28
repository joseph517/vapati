package com.vaPaTi.vaPaTi.repository.user;

import com.vaPaTi.vaPaTi.entity.user.UserSocialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSocialLinkRepository extends JpaRepository<UserSocialLink, Long> {
    // The links of one profile in one SELECT, outside the user's entity graph (it already fetches two collections).
    // UserMapper sorts them
    List<UserSocialLink> findByUserId(Long userId);
}
