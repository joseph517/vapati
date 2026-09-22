package com.vaPaTi.vaPaTi.mapper;

import com.vaPaTi.vaPaTi.dtos.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.Donation;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.validation.DonationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                .id(10L).donor(donor).campaign(campaign).amount(50.0).status(DonationStatus.COMPLETED)
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
    @DisplayName("Should keep the donation anonymous when the donor is deleted (relation null)")
    void toDTO_WithDeletedDonor_ShouldReturnNullDonorFields() {
        // Given
        Donation donation = Donation.builder()
                .id(10L).donor(null).donorUserId(1L).campaign(campaign).amount(50.0)
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
                .id(10L).donor(donor).campaign(null).campaignId(20L).amount(50.0)
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
                .id(10L).donor(null).donorUserId(1L).campaign(null).campaignId(20L).amount(50.0)
                .build();

        // When & Then
        assertThatCode(() -> donationMapper.toDTO(donation)).doesNotThrowAnyException();
        DonationResponseDTO dto = donationMapper.toDTO(donation);
        assertThat(dto.getCampaignId()).isEqualTo(20L);
        assertThat(dto.getCampaignName()).isNull();
        assertThat(dto.getCampaignDeleted()).isTrue();
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
}
