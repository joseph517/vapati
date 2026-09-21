package com.vaPaTi.vaPaTi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 50)
    private CampaignStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    private CampaignStatus newStatus;

    @ManyToOne
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
