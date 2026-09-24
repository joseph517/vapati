package com.vaPaTi.vaPaTi.entity;

import com.vaPaTi.vaPaTi.validation.DonationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // donor and campaign are optional in the mapping (the columns stay NOT NULL) so a soft-deleted
    // target is left null by @NotFound instead of inner-joining the row away. They are not updatable,
    // so an UPDATE never overwrites the FK with that null.
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "donor_user_id", updatable = false)
    private User donor;

    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "campaign_id", updatable = false)
    private Campaign campaign;

    // Read-only copies of the FKs: they keep their value when donor or campaign is soft deleted
    // (the relation is then null) and let queries filter by FK without joining the soft-deleted table.
    @Column(name = "campaign_id", insertable = false, updatable = false)
    private Long campaignId;

    @Column(name = "donor_user_id", insertable = false, updatable = false)
    private Long donorUserId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DonationStatus status = DonationStatus.PENDING;

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DonationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
