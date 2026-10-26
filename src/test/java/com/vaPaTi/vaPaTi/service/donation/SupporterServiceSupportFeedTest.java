package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.SupportFeedItemDTO;
import com.vaPaTi.vaPaTi.entity.donation.Donation;
import com.vaPaTi.vaPaTi.entity.donation.DonationStatus;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationMapper;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.donation.DonationValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupporterService - getSupportFeed() Tests")
class SupporterServiceSupportFeedTest {

    @Mock
    private DonationRepository donationRepository;
    @Mock
    private DonationValidationService donationValidationService;
    @Spy
    private DonationMapper donationMapper = new DonationMapper();
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private SupporterService supporterService;

    private static final Long CREATOR_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long THIRD_PARTY_ID = 3L;

    private User creator;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        creator = User.builder().id(CREATOR_ID).build();
        defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Nested
    @DisplayName("Creator validation")
    class CreatorValidationTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {0L, -1L})
        @DisplayName("Should throw MessageException when the id is not a positive number")
        void getSupportFeed_InvalidId_ShouldThrowMessageException(Long userId) {
            // When & Then
            assertThatThrownBy(() -> supporterService.getSupportFeed(userId, defaultPageable))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("ID must be a positive number");

            verify(userValidationService, never()).getUserById(any());
            assertFeedQueryNotCalled();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the creator doesn't exist")
        void getSupportFeed_CreatorNotFound_ShouldThrowResourceNotFoundException() {
            // Given
            when(userValidationService.getUserById(999L)).thenThrow(new ResourceNotFoundException("User not found"));

            // When & Then
            assertThatThrownBy(() -> supporterService.getSupportFeed(999L, defaultPageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            assertFeedQueryNotCalled();
        }
    }

    @Nested
    @DisplayName("Blocked creator")
    class BlockedCreatorTests {

        @Test
        @DisplayName("An anonymous caller should get 404 for a banned or suspended creator")
        void getSupportFeed_BlockedCreatorAsAnonymous_ShouldThrowResourceNotFoundException() {
            // Given
            givenCreatorExists();
            when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
            givenCreatorBlocked();

            // When & Then
            assertThatThrownBy(() -> supporterService.getSupportFeed(CREATOR_ID, defaultPageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            assertFeedQueryNotCalled();
        }

        @Test
        @DisplayName("Another USER should get 404 for a banned or suspended creator")
        void getSupportFeed_BlockedCreatorAsThirdParty_ShouldThrowResourceNotFoundException() {
            // Given
            givenCreatorExists();
            givenCaller(THIRD_PARTY_ID);
            when(userValidationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);
            givenCreatorBlocked();

            // When & Then
            assertThatThrownBy(() -> supporterService.getSupportFeed(CREATOR_ID, defaultPageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            assertFeedQueryNotCalled();
        }

        @Test
        @DisplayName("An ADMIN should get the feed of a banned or suspended creator")
        void getSupportFeed_BlockedCreatorAsAdmin_ShouldReturnFeed() {
            // Given
            givenCreatorExists();
            givenCaller(ADMIN_ID);
            when(userValidationService.isAdmin(ADMIN_ID)).thenReturn(true);
            givenFeed(List.of(donation(100L)), 1L);

            // When
            Page<SupportFeedItemDTO> result = supporterService.getSupportFeed(CREATOR_ID, defaultPageable);

            // Then
            assertThat(result.getContent()).extracting(SupportFeedItemDTO::getDonationId).containsExactly(100L);
            verify(userValidationService, never()).validateProfileNotBlocked(any());
        }

        @Test
        @DisplayName("The creator should get their own feed without the blocked check")
        void getSupportFeed_AsCreator_ShouldReturnFeed() {
            // Given
            givenCreatorExists();
            givenCaller(CREATOR_ID);
            givenFeed(List.of(donation(100L)), 1L);

            // When
            Page<SupportFeedItemDTO> result = supporterService.getSupportFeed(CREATOR_ID, defaultPageable);

            // Then
            assertThat(result.getContent()).extracting(SupportFeedItemDTO::getDonationId).containsExactly(100L);
            verify(userValidationService, never()).validateProfileNotBlocked(any());
        }

        private void givenCreatorBlocked() {
            doThrow(new ResourceNotFoundException("User not found"))
                    .when(userValidationService).validateProfileNotBlocked(creator);
        }
    }

    @Nested
    @DisplayName("Sort and pagination")
    class SortAndPaginationTests {

        @Test
        @DisplayName("Should query by createdAt DESC and then id DESC, with the same page and size")
        void getSupportFeed_ShouldAddIdDescTieBreaker() {
            // Given
            givenVisibleCreator();
            Pageable pageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
            givenFeed(List.of(), 10L);

            // When
            supporterService.getSupportFeed(CREATOR_ID, pageable);

            // Then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(donationRepository).findCompletedByRecipientId(eq(CREATOR_ID),
                    captor.capture());
            Pageable sent = captor.getValue();
            assertThat(sent.getPageNumber()).isEqualTo(2);
            assertThat(sent.getPageSize()).isEqualTo(5);
            assertThat(sent.getSort().toList()).containsExactly(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id"));
        }

        @Test
        @DisplayName("totalElements should come from the repository page")
        void getSupportFeed_ShouldKeepRepositoryTotal() {
            // Given
            givenVisibleCreator();
            givenFeed(List.of(donation(100L), donation(99L)), 42L);

            // When
            Page<SupportFeedItemDTO> result = supporterService.getSupportFeed(CREATOR_ID, defaultPageable);

            // Then
            assertThat(result.getTotalElements()).isEqualTo(42L);
            assertThat(result.getContent()).extracting(SupportFeedItemDTO::getDonationId)
                    .containsExactly(100L, 99L);
            verify(donationMapper, times(2)).toSupportFeedItemDTO(any(Donation.class));
        }

        @Test
        @DisplayName("Should return an empty page when the creator has no donations")
        void getSupportFeed_NoDonations_ShouldReturnEmptyPage() {
            // Given
            givenVisibleCreator();
            givenFeed(List.of(), 0L);

            // When
            Page<SupportFeedItemDTO> result = supporterService.getSupportFeed(CREATOR_ID, defaultPageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        private void givenVisibleCreator() {
            givenCreatorExists();
            givenCaller(THIRD_PARTY_ID);
            when(userValidationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);
        }
    }

    private void givenCreatorExists() {
        when(userValidationService.getUserById(CREATOR_ID)).thenReturn(creator);
    }

    private void givenCaller(Long callerId) {
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(callerId));
    }

    // The repository builds the page with the Pageable it receives, as Spring Data does
    private void givenFeed(List<Donation> donations, long total) {
        when(donationRepository.findCompletedByRecipientId(eq(CREATOR_ID), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(donations, invocation.getArgument(1), total));
    }

    private Donation donation(Long id) {
        return Donation.builder()
                .id(id)
                .amount(new BigDecimal("10.00"))
                .status(DonationStatus.COMPLETED)
                .recipientUserId(CREATOR_ID)
                .createdAt(LocalDateTime.of(2026, 10, 20, 10, 0))
                .build();
    }

    private void assertFeedQueryNotCalled() {
        verify(donationRepository, never()).findCompletedByRecipientId(anyLong(), any());
    }
}
