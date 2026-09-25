package com.vaPaTi.vaPaTi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "report",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"reporter_id", "reported_entity_type", "reported_entity_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @NotFound makes the relation eager: Hibernate must load it to know whether the user exists.
    // Optional in the mapping (the column stays NOT NULL) so a soft-deleted reporter is left null, not inner-joined away.
    // Not updatable, so an UPDATE never overwrites the FK with that null.
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "reporter_id", updatable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_entity_type", nullable = false, length = 50)
    private ReportedEntityType reportedEntityType;

    @Column(name = "reported_entity_id", nullable = false)
    private Long reportedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private ReportReason reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", length = 50)
    private ActionTaken actionTaken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
