package com.vaPaTi.vaPaTi.entity.donation;

import com.vaPaTi.vaPaTi.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// A creator's donation panel. One row per user at most, created by the first save: without a row the defaults apply
// (SIMPLE, DONATE, no unit, no minimum, no suggested amounts). User has no field back to this row
@Entity
@Table(name = "donation_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = "user")
public class DonationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 10)
    @Builder.Default
    private DonationMode mode = DonationMode.SIMPLE;

    // Only in UNIT mode, null in SIMPLE
    @Column(name = "unit_name", length = 30)
    private String unitName;

    // Only in UNIT mode, null in SIMPLE
    @Column(name = "unit_amount", precision = 15, scale = 2)
    private BigDecimal unitAmount;

    // null: no minimum besides being greater than zero
    @Column(name = "minimum_amount", precision = 15, scale = 2)
    private BigDecimal minimumAmount;

    // Sorted from lowest to highest and without gaps. Read and written only through getSuggestedAmounts and
    // replaceSuggestedAmounts, so no other class knows about the three columns
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "suggested_amount_1", precision = 15, scale = 2)
    private BigDecimal suggestedAmount1;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "suggested_amount_2", precision = 15, scale = 2)
    private BigDecimal suggestedAmount2;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "suggested_amount_3", precision = 15, scale = 2)
    private BigDecimal suggestedAmount3;

    @Enumerated(EnumType.STRING)
    @Column(name = "cta_text", nullable = false, length = 10)
    @Builder.Default
    private DonationCtaText ctaText = DonationCtaText.DONATE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // The only place of the effective minimum rule: minimumAmount in SIMPLE (unitAmount doesn't count), and the
    // greater of unitAmount and minimumAmount in UNIT, ignoring the one that is null. null means no minimum
    public BigDecimal getEffectiveMinimumAmount() {
        if (mode != DonationMode.UNIT) {
            return minimumAmount;
        }
        return Stream.of(unitAmount, minimumAmount)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    // The suggested amounts in column order, which is ascending
    public List<BigDecimal> getSuggestedAmounts() {
        return Stream.of(suggestedAmount1, suggestedAmount2, suggestedAmount3)
                .filter(Objects::nonNull)
                .toList();
    }

    // Takes an already sorted list of up to 3 amounts and fills the columns from the first one, leaving the rest null
    public void replaceSuggestedAmounts(List<BigDecimal> sortedAmounts) {
        List<BigDecimal> columns = new ArrayList<>(sortedAmounts);
        while (columns.size() < 3) {
            columns.add(null);
        }
        suggestedAmount1 = columns.get(0);
        suggestedAmount2 = columns.get(1);
        suggestedAmount3 = columns.get(2);
    }
}
