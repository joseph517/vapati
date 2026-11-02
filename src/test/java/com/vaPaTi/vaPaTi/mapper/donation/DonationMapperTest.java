package com.vaPaTi.vaPaTi.mapper.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.dtos.donation.SupportFeedItemDTO;
import com.vaPaTi.vaPaTi.dtos.donation.SupporterResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("DonationMapper Tests")
class DonationMapperTest {

    private final DonationMapper donationMapper = new DonationMapper();

    private User donor;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName("donor_user");

        donor = new User();
        donor.setId(1L);
        donor.setUserInfo(userInfo);

        campaign = new Campaign();
        campaign.setId(2L);
        campaign.setName("Alive campaign");
    }

    @Test
    @DisplayName("Should map donor and campaign data when both are alive, with campaignDeleted false")
    void toDTO_WithAliveRelations_ShouldMapAllFields() {
        // Given
        Donation donation = Donation.builder()
                .id(10L).donor(donor).campaign(campaign).amount(new BigDecimal("50.0")).status(DonationStatus.COMPLETED)
                .build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getDonorUserId()).isEqualTo(1L);
        assertThat(dto.getDonorUserName()).isEqualTo("donor_user");
        assertThat(dto.getCampaignId()).isEqualTo(2L);
        assertThat(dto.getCampaignName()).isEqualTo("Alive campaign");
        assertThat(dto.getCampaignDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should copy the recipient id and user name")
    void toDTO_WithRecipient_ShouldCopyRecipientFields() {
        // Given
        Donation donation = Donation.builder().id(10L).donor(donor).recipient(recipientWithName("owner_user", 7L))
                .campaign(campaign).amount(new BigDecimal("50.0")).build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getRecipientUserId()).isEqualTo(7L);
        assertThat(dto.getRecipientUserName()).isEqualTo("owner_user");
    }

    @Test
    @DisplayName("Should map recipient id and name to null when the recipient is deleted (null)")
    void toDTO_WithNullRecipient_ShouldReturnNullRecipientFields() {
        // Given
        Donation donation = Donation.builder().id(10L).donor(donor).recipient(null).campaign(campaign).build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getRecipientUserId()).isNull();
        assertThat(dto.getRecipientUserName()).isNull();
    }

    @Test
    @DisplayName("Should map recipientUserName to null when the recipient has no userInfo")
    void toDTO_WithRecipientWithoutUserInfo_ShouldReturnNullRecipientName() {
        // Given
        User recipient = new User();
        recipient.setId(8L);
        Donation donation = Donation.builder().id(10L).donor(donor).recipient(recipient).campaign(campaign).build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getRecipientUserId()).isEqualTo(8L);
        assertThat(dto.getRecipientUserName()).isNull();
    }

    @Test
    @DisplayName("Should map a direct donation to null campaign fields and campaignDeleted false without throwing")
    void toDTO_WithDirectDonation_ShouldReturnNullCampaignFieldsAndNotDeleted() {
        // Given
        Donation donation = Donation.builder().id(10L).donor(donor).recipient(recipientWithName("owner_user", 7L))
                .campaign(null).campaignId(null).amount(new BigDecimal("5.0")).build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation, Map.of());

        // Then
        assertThat(dto.getCampaignId()).isNull();
        assertThat(dto.getCampaignName()).isNull();
        assertThat(dto.getCampaignDeleted()).isFalse();
        assertThat(dto.getRecipientUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Should keep the donation anonymous when the donor is deleted (relation null)")
    void toDTO_WithDeletedDonor_ShouldReturnNullDonorFields() {
        // Given
        Donation donation = Donation.builder()
                .id(10L).donor(null).donorUserId(1L).campaign(campaign).amount(new BigDecimal("50.0"))
                .build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getDonorUserId()).isNull();
        assertThat(dto.getDonorUserName()).isNull();
        assertThat(dto.getCampaignId()).isEqualTo(2L);
        assertThat(dto.getCampaignDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should keep campaignId, take the name from the deleted names and mark campaignDeleted when the campaign is deleted")
    void toDTO_WithDeletedCampaign_ShouldUseReadOnlyIdAndDeletedName() {
        // Given
        Donation donation = Donation.builder()
                .id(10L).donor(donor).campaign(null).campaignId(20L).amount(new BigDecimal("50.0"))
                .build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation, Map.of(20L, "Deleted campaign"));

        // Then
        assertThat(dto.getCampaignId()).isEqualTo(20L);
        assertThat(dto.getCampaignName()).isEqualTo("Deleted campaign");
        assertThat(dto.getCampaignDeleted()).isTrue();
        assertThat(dto.getDonorUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should not throw when both donor and campaign are deleted")
    void toDTO_WithDeletedDonorAndCampaign_ShouldNotThrow() {
        // Given
        Donation donation = Donation.builder()
                .id(10L).donor(null).donorUserId(1L).campaign(null).campaignId(20L).amount(new BigDecimal("50.0"))
                .build();

        // When & Then
        assertThatCode(() -> donationMapper.toDTO(donation)).doesNotThrowAnyException();
        DonationResponseDTO dto = donationMapper.toDTO(donation);
        assertThat(dto.getCampaignId()).isEqualTo(20L);
        assertThat(dto.getCampaignName()).isNull();
        assertThat(dto.getCampaignDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should copy message and messagePrivate from the donation")
    void toDTO_WithPrivateMessage_ShouldCopyMessageFields() {
        // Given
        Donation donation = Donation.builder()
                .id(10L).donor(donor).campaign(campaign).amount(new BigDecimal("50.0"))
                .message("Solo para vos").messagePrivate(true)
                .build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getMessage()).isEqualTo("Solo para vos");
        assertThat(dto.getMessagePrivate()).isTrue();
    }

    @Test
    @DisplayName("Should map a donation without message to message null and messagePrivate false")
    void toDTO_WithoutMessage_ShouldMapNullMessageAndFalsePrivate() {
        // Given
        Donation donation = Donation.builder()
                .id(10L).donor(donor).campaign(campaign).amount(new BigDecimal("50.0"))
                .build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getMessage()).isNull();
        assertThat(dto.getMessagePrivate()).isFalse();
    }

    @Test
    @DisplayName("Should copy thanksMessage, thanksPrivate and thankedAt from the donation")
    void toDTO_WithThanks_ShouldCopyThanksFields() {
        // Given
        LocalDateTime thankedAt = LocalDateTime.of(2026, 10, 15, 12, 0);
        Donation donation = Donation.builder()
                .id(10L).donor(donor).campaign(campaign).amount(new BigDecimal("50.0"))
                .thanksMessage("Gracias, de verdad").thanksPrivate(true).thankedAt(thankedAt)
                .build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getThanksMessage()).isEqualTo("Gracias, de verdad");
        assertThat(dto.getThanksPrivate()).isTrue();
        assertThat(dto.getThankedAt()).isEqualTo(thankedAt);
    }

    @Test
    @DisplayName("Should map a donation without thanks to thanksMessage null, thanksPrivate false and thankedAt null")
    void toDTO_WithoutThanks_ShouldMapEmptyThanksFields() {
        // Given
        Donation donation = Donation.builder()
                .id(10L).donor(donor).campaign(campaign).amount(new BigDecimal("50.0"))
                .build();

        // When
        DonationResponseDTO dto = donationMapper.toDTO(donation);

        // Then
        assertThat(dto.getThanksMessage()).isNull();
        assertThat(dto.getThanksPrivate()).isFalse();
        assertThat(dto.getThankedAt()).isNull();
    }

    @Test
    @DisplayName("Should pass the deleted campaign names to every element of the list")
    void toDTOList_WithDeletedNames_ShouldMapEachDonation() {
        // Given
        Donation alive = Donation.builder().id(10L).donor(donor).campaign(campaign).build();
        Donation deleted = Donation.builder().id(11L).donor(donor).campaign(null).campaignId(20L).build();

        // When
        List<DonationResponseDTO> dtos = donationMapper.toDTOList(List.of(alive, deleted), Map.of(20L, "Deleted campaign"));

        // Then
        assertThat(dtos).extracting(DonationResponseDTO::getCampaignDeleted).containsExactly(false, true);
        assertThat(dtos).extracting(DonationResponseDTO::getCampaignName).containsExactly("Alive campaign", "Deleted campaign");
    }

    @Test
    @DisplayName("Should copy the four aggregates and the donor into the supporter row")
    void toSupporterDTO_WithAliveDonor_ShouldCopyAggregatesAndDonor() {
        // Given
        LocalDateTime lastDonationAt = LocalDateTime.of(2026, 10, 15, 12, 0);
        DonationRepository.SupporterProjection row = supporterRow(1L, new BigDecimal("75.50"), 3L, lastDonationAt);

        // When
        SupporterResponseDTO dto = donationMapper.toSupporterDTO(row, donor);

        // Then
        assertThat(dto.getDonorUserId()).isEqualTo(1L);
        assertThat(dto.getDonorUserName()).isEqualTo("donor_user");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("75.50");
        assertThat(dto.getDonationsCount()).isEqualTo(3L);
        assertThat(dto.getLastDonationAt()).isEqualTo(lastDonationAt);
    }

    @Test
    @DisplayName("Should keep the supporter anonymous and keep the aggregates when the donor is deleted (null)")
    void toSupporterDTO_WithDeletedDonor_ShouldReturnNullDonorFieldsAndKeepAggregates() {
        // Given
        LocalDateTime lastDonationAt = LocalDateTime.of(2026, 10, 15, 12, 0);
        DonationRepository.SupporterProjection row = supporterRow(1L, new BigDecimal("40.00"), 2L, lastDonationAt);

        // When
        SupporterResponseDTO dto = donationMapper.toSupporterDTO(row, null);

        // Then
        assertThat(dto.getDonorUserId()).isNull();
        assertThat(dto.getDonorUserName()).isNull();
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("40.00");
        assertThat(dto.getDonationsCount()).isEqualTo(2L);
        assertThat(dto.getLastDonationAt()).isEqualTo(lastDonationAt);
    }

    @Test
    @DisplayName("Should map donorUserName to null when the donor has no userInfo")
    void toSupporterDTO_WithDonorWithoutUserInfo_ShouldReturnNullUserName() {
        // Given
        User donorWithoutInfo = new User();
        donorWithoutInfo.setId(5L);
        DonationRepository.SupporterProjection row =
                supporterRow(5L, new BigDecimal("10.00"), 1L, LocalDateTime.of(2026, 10, 15, 12, 0));

        // When
        SupporterResponseDTO dto = donationMapper.toSupporterDTO(row, donorWithoutInfo);

        // Then
        assertThat(dto.getDonorUserId()).isEqualTo(5L);
        assertThat(dto.getDonorUserName()).isNull();
    }

    private User recipientWithName(String userName, Long id) {
        UserInfo info = new UserInfo();
        info.setUserName(userName);
        User recipient = new User();
        recipient.setId(id);
        recipient.setUserInfo(info);
        return recipient;
    }

    private DonationRepository.SupporterProjection supporterRow(Long donorUserId, BigDecimal totalAmount,
                                                                Long donationsCount, LocalDateTime lastDonationAt) {
        return new DonationRepository.SupporterProjection() {
            @Override
            public Long getDonorUserId() {
                return donorUserId;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return totalAmount;
            }

            @Override
            public Long getDonationsCount() {
                return donationsCount;
            }

            @Override
            public LocalDateTime getLastDonationAt() {
                return lastDonationAt;
            }
        };
    }

    @Test
    @DisplayName("Feed item: should copy donor, campaign, message, thanks and dates")
    void toSupportFeedItemDTO_WithAliveRelations_ShouldCopyFields() {
        // Given
        LocalDateTime thankedAt = LocalDateTime.of(2026, 3, 2, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        Donation donation = Donation.builder()
                .id(10L).donor(donor).campaign(campaign).campaignId(2L)
                .message("Keep going").thanksMessage("Thank you").thankedAt(thankedAt).createdAt(createdAt)
                .build();

        // When
        SupportFeedItemDTO dto = donationMapper.toSupportFeedItemDTO(donation);

        // Then
        assertThat(dto.getDonationId()).isEqualTo(10L);
        assertThat(dto.getDonorUserId()).isEqualTo(1L);
        assertThat(dto.getDonorUserName()).isEqualTo("donor_user");
        assertThat(dto.getCampaignId()).isEqualTo(2L);
        assertThat(dto.getCampaignName()).isEqualTo("Alive campaign");
        assertThat(dto.getMessage()).isEqualTo("Keep going");
        assertThat(dto.getThanksMessage()).isEqualTo("Thank you");
        assertThat(dto.getThankedAt()).isEqualTo(thankedAt);
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Feed item: should leave donor id and name null when the donor is deleted (null)")
    void toSupportFeedItemDTO_WithNullDonor_ShouldLeaveDonorFieldsNull() {
        Donation donation = Donation.builder().id(10L).donor(null).donorUserId(1L).build();

        SupportFeedItemDTO dto = donationMapper.toSupportFeedItemDTO(donation);

        assertThat(dto.getDonorUserId()).isNull();
        assertThat(dto.getDonorUserName()).isNull();
    }

    @Test
    @DisplayName("Feed item: should leave donorUserName null when the donor has no userInfo")
    void toSupportFeedItemDTO_WithDonorWithoutUserInfo_ShouldLeaveNameNull() {
        User noInfo = new User();
        noInfo.setId(5L);
        Donation donation = Donation.builder().id(10L).donor(noInfo).build();

        SupportFeedItemDTO dto = donationMapper.toSupportFeedItemDTO(donation);

        assertThat(dto.getDonorUserId()).isEqualTo(5L);
        assertThat(dto.getDonorUserName()).isNull();
    }

    @Test
    @DisplayName("Feed item: should keep campaignId and leave campaignName null when the campaign relation is null")
    void toSupportFeedItemDTO_WithNullCampaignRelation_ShouldKeepCampaignId() {
        Donation donation = Donation.builder().id(10L).donor(donor).campaign(null).campaignId(20L).build();

        SupportFeedItemDTO dto = donationMapper.toSupportFeedItemDTO(donation);

        assertThat(dto.getCampaignId()).isEqualTo(20L);
        assertThat(dto.getCampaignName()).isNull();
    }

    @Test
    @DisplayName("Feed item: should leave campaign fields null on a direct donation")
    void toSupportFeedItemDTO_WithDirectDonation_ShouldLeaveCampaignFieldsNull() {
        Donation donation = Donation.builder().id(10L).donor(donor).campaign(null).campaignId(null).build();

        SupportFeedItemDTO dto = donationMapper.toSupportFeedItemDTO(donation);

        assertThat(dto.getCampaignId()).isNull();
        assertThat(dto.getCampaignName()).isNull();
    }

    @Test
    @DisplayName("Feed item: campaignHidden should always be false")
    void toSupportFeedItemDTO_ShouldAlwaysSetCampaignHiddenFalse() {
        Donation withCampaign = Donation.builder().id(10L).donor(donor).campaign(campaign).campaignId(2L).build();
        Donation withDeletedCampaign = Donation.builder().id(11L).donor(donor).campaign(null).campaignId(20L).build();
        Donation direct = Donation.builder().id(12L).donor(donor).build();

        assertThat(donationMapper.toSupportFeedItemDTO(withCampaign).getCampaignHidden()).isFalse();
        assertThat(donationMapper.toSupportFeedItemDTO(withDeletedCampaign).getCampaignHidden()).isFalse();
        assertThat(donationMapper.toSupportFeedItemDTO(direct).getCampaignHidden()).isFalse();
    }

    @Test
    @DisplayName("Feed item: should copy message and thanks even if private, the mapper does not decide")
    void toSupportFeedItemDTO_WithPrivateTexts_ShouldStillCopyThem() {
        Donation donation = Donation.builder().id(10L).donor(donor)
                .message("secret").messagePrivate(true).thanksMessage("secret thanks").thanksPrivate(true).build();

        SupportFeedItemDTO dto = donationMapper.toSupportFeedItemDTO(donation);

        assertThat(dto.getMessage()).isEqualTo("secret");
        assertThat(dto.getThanksMessage()).isEqualTo("secret thanks");
    }

    @Test
    @DisplayName("Feed item: should return null when the donation is null")
    void toSupportFeedItemDTO_WithNull_ShouldReturnNull() {
        assertThat(donationMapper.toSupportFeedItemDTO(null)).isNull();
    }
}
