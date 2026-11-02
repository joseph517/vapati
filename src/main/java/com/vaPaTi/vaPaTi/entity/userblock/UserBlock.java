package com.vaPaTi.vaPaTi.entity.userblock;

import com.vaPaTi.vaPaTi.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

// "blocker blocks blockedUser". The opposite direction is another, independent row. No soft delete: unblocking
// deletes the row. The FKs have no cascade, so a deleted account keeps its rows and gets them back if restored
@Entity
@Table(name = "user_block",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_block_blocker_blocked",
                columnNames = {"blocker_id", "blocked_user_id"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The checks compare ids only, so neither user is loaded for them
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_user_id", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User blockedUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
