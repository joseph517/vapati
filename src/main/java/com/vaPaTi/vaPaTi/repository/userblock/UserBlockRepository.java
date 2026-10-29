package com.vaPaTi.vaPaTi.repository.userblock;

import com.vaPaTi.vaPaTi.entity.userblock.UserBlock;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    // A block in either direction between two users. Both branches match the columns of the unique constraint
    @Query("SELECT COUNT(ub) > 0 FROM UserBlock ub " +
            "WHERE (ub.blocker.id = :userId AND ub.blockedUser.id = :otherUserId) " +
            "OR (ub.blocker.id = :otherUserId AND ub.blockedUser.id = :userId)")
    boolean existsBlockBetween(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    // Only "blocker blocks blockedUser", not the other direction
    @Query("SELECT COUNT(ub) > 0 FROM UserBlock ub " +
            "WHERE ub.blocker.id = :blockerId AND ub.blockedUser.id = :blockedUserId")
    boolean existsByBlockerIdAndBlockedUserId(@Param("blockerId") Long blockerId,
                                              @Param("blockedUserId") Long blockedUserId);

    // The row itself, by ids: the blocked user isn't loaded, so it's found even if deleted or sanctioned
    @Query("SELECT ub FROM UserBlock ub WHERE ub.blocker.id = :blockerId AND ub.blockedUser.id = :blockedUserId")
    Optional<UserBlock> findByBlockerIdAndBlockedUserId(@Param("blockerId") Long blockerId,
                                                        @Param("blockedUserId") Long blockedUserId);

    // The blocks of a user, without deleted, banned or currently suspended blocked users. The blocked user comes in
    // the same SELECT; only to-one fetches, so paging stays in SQL and the FETCH needs an explicit countQuery
    @Query(value = "SELECT ub FROM UserBlock ub JOIN FETCH ub.blockedUser u JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.verificationRequest " +
            "WHERE ub.blocker.id = :blockerId AND u.deletedAt IS NULL AND " + UserRepository.NOT_SANCTIONED,
            countQuery = "SELECT COUNT(ub) FROM UserBlock ub JOIN ub.blockedUser u " +
                    "WHERE ub.blocker.id = :blockerId AND u.deletedAt IS NULL AND " + UserRepository.NOT_SANCTIONED)
    Page<UserBlock> findVisibleByBlockerId(@Param("blockerId") Long blockerId, @Param("now") LocalDateTime now,
                                           Pageable pageable);
}
