package com.vaPaTi.vaPaTi.entity.comment;

import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment",
        indexes = @Index(name = "idx_comment_publication_created", columnList = "publication_id, created_at"))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE comment SET deleted_at = GETDATE() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text", nullable = false, length = 280)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Optional in the mapping (the column stays NOT NULL) so a soft-deleted author is left null, not inner-joined away.
    // Not updatable, so an UPDATE (the soft delete) never overwrites the FK with that null
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "user_id", updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    // Only navigated once the publication is known to be visible; checks and queries use publicationId
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Publication publication;

    // Read-only copy of the FK: keeps its value when the publication is soft deleted and lets queries filter without a join
    @Column(name = "publication_id", insertable = false, updatable = false)
    private Long publicationId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
