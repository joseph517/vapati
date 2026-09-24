package com.vaPaTi.vaPaTi.repository;

import com.vaPaTi.vaPaTi.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    // Atomic add in the database, so concurrent donations never overwrite each other's sum.
    // Returns 0 if the goal was CLOSED after the donation was validated. A bulk update skips
    // @UpdateTimestamp/@PreUpdate, so updatedAt is set here.
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Goal g SET g.amountRaised = g.amountRaised + :amount, g.updatedAt = :now "
            + "WHERE g.id = :goalId AND g.status <> com.vaPaTi.vaPaTi.entity.CampaignStatus.CLOSED")
    int addToAmountRaised(@Param("goalId") Long goalId, @Param("amount") BigDecimal amount, @Param("now") LocalDateTime now);

    // Returns 1 only for the donation that moved the goal from ACTIVE to COMPLETED
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Goal g SET g.status = com.vaPaTi.vaPaTi.entity.CampaignStatus.COMPLETED, g.updatedAt = :now "
            + "WHERE g.id = :goalId AND g.status = com.vaPaTi.vaPaTi.entity.CampaignStatus.ACTIVE "
            + "AND g.amountRaised >= g.amountGoal")
    int completeIfGoalReached(@Param("goalId") Long goalId, @Param("now") LocalDateTime now);
}
