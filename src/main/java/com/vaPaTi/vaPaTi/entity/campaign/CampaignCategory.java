package com.vaPaTi.vaPaTi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(name = "campaign_category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "campaign")
@ToString(exclude = "campaign")
public class CampaignCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional in the mapping (the column stays NOT NULL) so a soft-deleted campaign is left null, not inner-joined away.
    // Not updatable, so an UPDATE never overwrites the FK with that null.
    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "campaign_id", updatable = false)
    private Campaign campaign;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
