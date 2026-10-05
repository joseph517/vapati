package com.vaPaTi.vaPaTi.repository.follower;

import com.vaPaTi.vaPaTi.entity.follower.Follower;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowerRepository extends JpaRepository<Follower, Long> {

    // Verify if a user already follows another one
    boolean existsByUserAndFollower(User user, User follower);

    // Get the specific relationship in order to delete it
    Optional<Follower> findByUserAndFollower(User user, User follower);

    // Get all followers of a user (who follows him). The inner joins keep deleted followers out; verificationRequest
    // is a LEFT JOIN FETCH so its inverse @OneToOne doesn't cost a SELECT per row
    @Query("SELECT f FROM Follower f JOIN FETCH f.follower u JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.verificationRequest WHERE f.user = :user")
    List<Follower> findFollowersByUser(@Param("user") User user);

    // Get all users that a user follows (who he follows). Same joins as findFollowersByUser
    @Query("SELECT f FROM Follower f JOIN FETCH f.user u JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.verificationRequest WHERE f.follower = :follower")
    List<Follower> findFollowingsByFollower(@Param("follower") User follower);

    // Count followers, excluding deleted ones (same rule as findFollowersByUser)
    @Query("SELECT COUNT(f) FROM Follower f JOIN f.follower u WHERE f.user = :user AND u.deletedAt IS NULL")
    long countByUser(@Param("user") User user);

    // Count following, excluding deleted users (same rule as findFollowingsByFollower)
    @Query("SELECT COUNT(f) FROM Follower f JOIN f.user u WHERE f.follower = :follower AND u.deletedAt IS NULL")
    long countByFollower(@Param("follower") User follower);

    // *Visible* queries are for non-admins: same as the ones above, without banned or currently suspended users

    @Query("SELECT f FROM Follower f JOIN FETCH f.follower u JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.verificationRequest WHERE f.user = :user AND " + UserRepository.NOT_BLOCKED)
    List<Follower> findVisibleFollowersByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM Follower f JOIN FETCH f.user u JOIN FETCH u.userInfo " +
            "LEFT JOIN FETCH u.verificationRequest WHERE f.follower = :follower AND " + UserRepository.NOT_BLOCKED)
    List<Follower> findVisibleFollowingsByFollower(@Param("follower") User follower, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(f) FROM Follower f JOIN f.follower u WHERE f.user = :user AND u.deletedAt IS NULL AND " +
            UserRepository.NOT_BLOCKED)
    long countVisibleByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(f) FROM Follower f JOIN f.user u WHERE f.follower = :follower AND u.deletedAt IS NULL AND " +
            UserRepository.NOT_BLOCKED)
    long countVisibleByFollower(@Param("follower") User follower, @Param("now") LocalDateTime now);

}
