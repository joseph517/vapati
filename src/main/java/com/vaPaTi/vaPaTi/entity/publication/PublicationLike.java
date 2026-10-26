package com.vaPaTi.vaPaTi.entity.publication;

import com.vaPaTi.vaPaTi.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "publication_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_publication_like_publication_user",
                columnNames = {"publication_id", "user_id"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicationLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Never loaded to be shown: the likes are only counted or matched by id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Publication publication;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
