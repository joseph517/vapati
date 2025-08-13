package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Follower;
import com.vaPaTi.vaPaTi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowerRepository extends JpaRepository<Follower, Long> {

    // Verify if a user already follows another one
    boolean existsByUserAndFollower(User user, User follower);

    // Get the specific relationship in order to delete it
    Optional<Follower> findByUserAndFollower(User user, User follower);

    // Get all followers of a user (who follows him)
    @Query("SELECT f FROM Follower f JOIN FETCH f.follower JOIN FETCH f.follower.userInfo WHERE f.user = :user")
    List<Follower> findFollowersByUser(@Param("user") User user);

    // Get all users that a user follows (who he follows)
    @Query("SELECT f FROM Follower f JOIN FETCH f.user JOIN FETCH f.user.userInfo WHERE f.follower = :follower")
    List<Follower> findFollowingsByFollower(@Param("follower") User follower);

    // Count followers
    long countByUser(User user);

    // Count following
    long countByFollower(User follower);

}
