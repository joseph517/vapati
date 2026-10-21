package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.DonationResponseDTO;
import com.vaPaTi.vaPaTi.entity.campaign.Campaign;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.mapper.donation.DonationMapper;
import com.vaPaTi.vaPaTi.repository.campaign.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.campaign.GoalRepository;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.campaign.CampaignStatusHistoryService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.donation.DonationValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationService - Donations received by the authenticated user")
class DonationServiceReceivedTest {

    @Mock
    private DonationRepository donationRepository;
    @Mock
    private DonationValidationService donationValidationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    // Real mapper: the test checks that nothing is hidden on the way to the DTO
    @Spy
    private DonationMapper donationMapper = new DonationMapper();
    @Mock
    private CampaignStatusHistoryService campaignStatusHistoryService;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private CampaignAuthorizationService campaignAuthorizationService;

    @InjectMocks
    private DonationService donationService;

    private static final Long RECIPIENT_ID = 2L;
    private static final Long DONOR_ID = 1L;
    private static final Long DELETED_CAMPAIGN_ID = 20L;

    private User recipient;
    private User donor;

    @BeforeEach
    void setUp() {
        recipient = User.builder().id(RECIPIENT_ID).build();
        recipient.setUserInfo(UserInfo.builder().id(20L).userName("recipient_user").build());
        donor = User.builder().id(DONOR_ID).build();
        donor.setUserInfo(UserInfo.builder().id(10L).userName("donor_user").build());
    }

    private Donation directDonation(Long id) {
        return Donation.builder()
                .id(id)
                .donor(donor)
                .recipient(recipient)
                .recipientUserId(RECIPIENT_ID)
                .amount(new BigDecimal("5.00"))
                .status(DonationStatus.COMPLETED)
                .transactionId("TXN-" + id)
                .build();
    }

    private Donation deletedCampaignDonation(Long id) {
        Donation donation = directDonation(id);
        donation.setCampaign(null);
        donation.setCampaignId(DELETED_CAMPAIGN_ID);
        return donation;
    }

    private void givenReceived(Page<Donation> page) {
        when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(RECIPIENT_ID);
        when(donationRepository.findByRecipientId(eq(RECIPIENT_ID), any(Pageable.class))).thenReturn(page);
    }

    @Test
    @DisplayName("Should query by the token's user id with the requested page, the requested order and id desc as tie-breaker")
    void getDonationsReceived_ShouldPassTokenIdAndSortWithIdTieBreaker() {
        // Given
        givenReceived(Page.empty());
        Pageable requested = PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "amount"));

        // When
        donationService.getDonationsReceived(requested);

        // Then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(donationRepository).findByRecipientId(eq(RECIPIENT_ID), captor.capture());
        Pageable sent = captor.getValue();
        assertThat(sent.getPageNumber()).isEqualTo(2);
        assertThat(sent.getPageSize()).isEqualTo(5);
        assertThat(sent.getSort().toList()).containsExactly(
                Sort.Order.asc("amount"),
                Sort.Order.desc("id"));
    }

    @Test
    @DisplayName("Should not hide the transactionId, the private message or the private reply")
    void getDonationsReceived_ShouldNotHidePrivateData() {
        // Given
        LocalDateTime thankedAt = LocalDateTime.of(2026, 10, 16, 10, 0);
        Donation donation = directDonation(30L);
        donation.setMessage("Private message");
        donation.setMessagePrivate(true);
        donation.setThanksMessage("Private reply");
        donation.setThanksPrivate(true);
        donation.setThankedAt(thankedAt);
        givenReceived(new PageImpl<>(List.of(donation), PageRequest.of(0, 10), 1));

        // When
        Page<DonationResponseDTO> result = donationService.getDonationsReceived(PageRequest.of(0, 10, Sort.by("createdAt")));

        // Then
        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getTransactionId()).isEqualTo("TXN-30");
            assertThat(dto.getMessage()).isEqualTo("Private message");
            assertThat(dto.getMessagePrivate()).isTrue();
            assertThat(dto.getThanksMessage()).isEqualTo("Private reply");
            assertThat(dto.getThanksPrivate()).isTrue();
            assertThat(dto.getThankedAt()).isEqualTo(thankedAt);
            assertThat(dto.getRecipientUserId()).isEqualTo(RECIPIENT_ID);
            assertThat(dto.getRecipientUserName()).isEqualTo("recipient_user");
            assertThat(dto.getDonorUserName()).isEqualTo("donor_user");
            assertThat(dto.getCampaignId()).isNull();
            assertThat(dto.getCampaignDeleted()).isFalse();
        });
    }

    @Test
    @DisplayName("Should show donations to a deleted campaign with its name, fetched in a single query")
    void getDonationsReceived_WithDeletedCampaign_ShouldFetchNamesInSingleQuery() {
        // Given: two donations to the same deleted campaign, one to an alive campaign and one direct
        Campaign aliveCampaign = Campaign.builder().id(21L).name("Alive campaign").user(recipient).build();
        Donation toAlive = directDonation(43L);
        toAlive.setCampaign(aliveCampaign);
        toAlive.setCampaignId(21L);
        List<Donation> donations = List.of(
                deletedCampaignDonation(40L), deletedCampaignDonation(41L), directDonation(42L), toAlive);
        givenReceived(new PageImpl<>(donations, PageRequest.of(0, 10), 4));

        CampaignRepository.CampaignNameProjection projection = mock(CampaignRepository.CampaignNameProjection.class);
        when(projection.getId()).thenReturn(DELETED_CAMPAIGN_ID);
        when(projection.getName()).thenReturn("Deleted campaign");
        when(campaignRepository.findNamesByIdsIncludingDeleted(List.of(DELETED_CAMPAIGN_ID))).thenReturn(List.of(projection));

        // When
        Page<DonationResponseDTO> result = donationService.getDonationsReceived(PageRequest.of(0, 10, Sort.by("createdAt")));

        // Then
        verify(campaignRepository, times(1)).findNamesByIdsIncludingDeleted(any());
        assertThat(result.getContent()).extracting(DonationResponseDTO::getCampaignName)
                .containsExactly("Deleted campaign", "Deleted campaign", null, "Alive campaign");
        assertThat(result.getContent()).extracting(DonationResponseDTO::getCampaignDeleted)
                .containsExactly(true, true, false, false);
    }

    @Test
    @DisplayName("Should not query deleted campaign names when there are only direct donations")
    void getDonationsReceived_WithOnlyDirectDonations_ShouldNotQueryNames() {
        // Given
        givenReceived(new PageImpl<>(List.of(directDonation(50L)), PageRequest.of(0, 10), 1));

        // When
        donationService.getDonationsReceived(PageRequest.of(0, 10, Sort.by("createdAt")));

        // Then
        verify(campaignRepository, never()).findNamesByIdsIncludingDeleted(any());
    }

    @Test
    @DisplayName("Should return an empty page keeping totalElements, without querying names")
    void getDonationsReceived_WithEmptyPage_ShouldKeepTotalAndNotQueryNames() {
        // Given: a page past the end, the total still counts every donation received
        givenReceived(new PageImpl<>(List.of(), PageRequest.of(3, 10), 7));

        // When
        Page<DonationResponseDTO> result = donationService.getDonationsReceived(PageRequest.of(3, 10, Sort.by("createdAt")));

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getNumber()).isEqualTo(3);
        verifyNoInteractions(campaignRepository);
    }
}
