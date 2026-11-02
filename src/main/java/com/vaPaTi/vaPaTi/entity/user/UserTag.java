package com.vaPaTi.vaPaTi.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tag"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "user")
public class UserTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    // Already normalized (lowercase, trimmed); natural key inside the user's set
    @EqualsAndHashCode.Include
    @Column(name = "tag", nullable = false, length = 30, updatable = false)
    private String tag;
}
