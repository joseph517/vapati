package com.vaPaTi.vaPaTi.repository.donation;

import com.vaPaTi.vaPaTi.entity.donation.DonationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonationSettingsRepository extends JpaRepository<DonationSettings, Long> {

    // Empty when the user never saved a configuration: the defaults apply
    Optional<DonationSettings> findByUserId(Long userId);
}
