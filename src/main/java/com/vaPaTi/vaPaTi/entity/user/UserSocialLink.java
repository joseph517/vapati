package com.vaPaTi.vaPaTi.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_social_link",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "platform"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "user")
public class UserSocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    // Natural key inside the user's set: one link per platform, and it never changes (another platform is another row).
    // equals/hashCode use only this field, so changing the url of a link already in the HashSet doesn't break the set
    @EqualsAndHashCode.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20, updatable = false)
    private SocialPlatform platform;

    @Column(name = "url", nullable = false)
    private String url;
}
