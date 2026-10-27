package com.vaPaTi.vaPaTi.service.donation;

import com.vaPaTi.vaPaTi.dtos.donation.SupporterResponseDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.donation.DonationMapper;
import com.vaPaTi.vaPaTi.repository.donation.DonationRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
import com.vaPaTi.vaPaTi.validation.donation.DonationValidationService;
import com.vaPaTi.vaPaTi.validation.user.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupporterService - getSupporters() Tests")
class SupporterServiceTest {

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

    @Mock
    private RoleValidationService roleValidationService;

    @InjectMocks
    private SupporterService supporterService;

    private static final Long CREATOR_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long THIRD_PARTY_ID = 3L;
    private static final Long DONOR_A_ID = 10L;
    private static final Long DONOR_B_ID = 11L;
    private static final LocalDateTime LAST_DONATION_A = LocalDateTime.of(2026, 10, 15, 12, 0);
    private static final LocalDateTime LAST_DONATION_B = LocalDateTime.of(2026, 10, 14, 9, 30);

    private User creator;
    private User donorA;
    private User donorB;
    private DonationRepository.SupporterProjection rowA;
    private DonationRepository.SupporterProjection rowB;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        creator = userWithName(CREATOR_ID, "creator_user");
        donorA = userWithName(DONOR_A_ID, "donor_a");
        donorB = userWithName(DONOR_B_ID, "donor_b");
        rowA = supporterRow(DONOR_A_ID, new BigDecimal("75.50"), 3L, LAST_DONATION_A);
        rowB = supporterRow(DONOR_B_ID, new BigDecimal("40.00"), 2L, LAST_DONATION_B);
        defaultPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "totalAmount"));
    }

    @Nested
    @DisplayName("Visibility of the aggregates")
    class VisibilityTests {

        @Test
        @DisplayName("The creator should receive the donors and the aggregates")
        void getSupporters_AsCreator_ShouldReturnAggregates() {
            // Given
            givenCreatorExists();
            givenCaller(CREATOR_ID);
            givenSupporters(List.of(rowA, rowB), 2L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID, DONOR_B_ID)))
                    .thenReturn(List.of(donorA, donorB));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            assertFullRows(result);
            verify(userValidationService, never()).validateProfileNotSanctioned(any());
        }

        @Test
        @DisplayName("An ADMIN should receive the donors and the aggregates")
        void getSupporters_AsAdmin_ShouldReturnAggregates() {
            // Given
            givenCreatorExists();
            givenCaller(ADMIN_ID);
            when(roleValidationService.isAdmin(ADMIN_ID)).thenReturn(true);
            givenSupporters(List.of(rowA, rowB), 2L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID, DONOR_B_ID)))
                    .thenReturn(List.of(donorA, donorB));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            assertFullRows(result);
            verify(userValidationService, never()).validateProfileNotSanctioned(any());
        }

        @Test
        @DisplayName("Another USER should receive the donors with the aggregates in null")
        void getSupporters_AsThirdParty_ShouldHideAggregates() {
            // Given
            givenCreatorExists();
            givenCaller(THIRD_PARTY_ID);
            when(roleValidationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);
            givenSupporters(List.of(rowA, rowB), 2L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID, DONOR_B_ID)))
                    .thenReturn(List.of(donorA, donorB));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            assertPublicRows(result);
            verify(userValidationService).validateProfileNotSanctioned(creator);
        }

        @Test
        @DisplayName("An anonymous caller should receive the donors with the aggregates in null")
        void getSupporters_AsAnonymous_ShouldHideAggregates() {
            // Given
            givenCreatorExists();
            givenAnonymousCaller();
            givenSupporters(List.of(rowA, rowB), 2L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID, DONOR_B_ID)))
                    .thenReturn(List.of(donorA, donorB));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            assertPublicRows(result);
            verify(userValidationService).validateProfileNotSanctioned(creator);
        }

        private void assertFullRows(Page<SupporterResponseDTO> result) {
            assertThat(result.getContent()).hasSize(2);

            SupporterResponseDTO first = result.getContent().get(0);
            assertThat(first.getDonorUserId()).isEqualTo(DONOR_A_ID);
            assertThat(first.getDonorUserName()).isEqualTo("donor_a");
            assertThat(first.getTotalAmount()).isEqualByComparingTo("75.50");
            assertThat(first.getDonationsCount()).isEqualTo(3L);
            assertThat(first.getLastDonationAt()).isEqualTo(LAST_DONATION_A);

            SupporterResponseDTO second = result.getContent().get(1);
            assertThat(second.getDonorUserId()).isEqualTo(DONOR_B_ID);
            assertThat(second.getDonorUserName()).isEqualTo("donor_b");
            assertThat(second.getTotalAmount()).isEqualByComparingTo("40.00");
            assertThat(second.getDonationsCount()).isEqualTo(2L);
            assertThat(second.getLastDonationAt()).isEqualTo(LAST_DONATION_B);
        }

        private void assertPublicRows(Page<SupporterResponseDTO> result) {
            assertThat(result.getContent())
                    .extracting(SupporterResponseDTO::getDonorUserId, SupporterResponseDTO::getDonorUserName)
                    .containsExactly(
                            tuple(DONOR_A_ID, "donor_a"),
                            tuple(DONOR_B_ID, "donor_b"));
            assertThat(result.getContent()).allSatisfy(dto -> {
                assertThat(dto.getTotalAmount()).isNull();
                assertThat(dto.getDonationsCount()).isNull();
                assertThat(dto.getLastDonationAt()).isNull();
            });
        }
    }

    @Nested
    @DisplayName("Inactive donors")
    class InactiveDonorTests {

        @Test
        @DisplayName("A donor that doesn't come back from findAllWithDetailsByIdIn should be anonymous and keep the aggregates in the full view")
        void getSupporters_WithDeletedDonorInFullView_ShouldBeAnonymousWithAggregates() {
            // Given
            givenCreatorExists();
            givenCaller(CREATOR_ID);
            givenSupporters(List.of(rowA, rowB), 2L);
            // donorB is soft deleted: the @SQLRestriction of User leaves it out
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID, DONOR_B_ID))).thenReturn(List.of(donorA));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            SupporterResponseDTO deleted = result.getContent().get(1);
            assertThat(deleted.getDonorUserId()).isNull();
            assertThat(deleted.getDonorUserName()).isNull();
            assertThat(deleted.getTotalAmount()).isEqualByComparingTo("40.00");
            assertThat(deleted.getDonationsCount()).isEqualTo(2L);
            assertThat(deleted.getLastDonationAt()).isEqualTo(LAST_DONATION_B);
            assertThat(result.getContent().get(0).getDonorUserId()).isEqualTo(DONOR_A_ID);
        }

        @Test
        @DisplayName("A deleted donor should be anonymous and without aggregates in the public view")
        void getSupporters_WithDeletedDonorInPublicView_ShouldBeAnonymousWithoutAggregates() {
            // Given
            givenCreatorExists();
            givenAnonymousCaller();
            givenSupporters(List.of(rowB), 1L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_B_ID))).thenReturn(List.of());

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            SupporterResponseDTO deleted = result.getContent().get(0);
            assertThat(deleted.getDonorUserId()).isNull();
            assertThat(deleted.getDonorUserName()).isNull();
            assertThat(deleted.getTotalAmount()).isNull();
            assertThat(deleted.getDonationsCount()).isNull();
            assertThat(deleted.getLastDonationAt()).isNull();
        }

        @Test
        @DisplayName("A banned donor should appear normally, with its id and userName")
        void getSupporters_WithBannedDonor_ShouldAppearNormally() {
            // Given
            donorA.setBanned(true);
            donorA.setBannedAt(LocalDateTime.of(2026, 10, 1, 8, 0));
            donorA.setBannedReason("Spam");
            givenCreatorExists();
            givenAnonymousCaller();
            givenSupporters(List.of(rowA), 1L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID))).thenReturn(List.of(donorA));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            SupporterResponseDTO banned = result.getContent().get(0);
            assertThat(banned.getDonorUserId()).isEqualTo(DONOR_A_ID);
            assertThat(banned.getDonorUserName()).isEqualTo("donor_a");
        }
    }

    @Nested
    @DisplayName("Creator not visible")
    class CreatorNotVisibleTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the creator doesn't exist")
        void getSupporters_WhenCreatorNotFound_ShouldThrowResourceNotFoundException() {
            // Given
            when(userValidationService.getUserById(999L)).thenThrow(new ResourceNotFoundException("User not found"));

            // When & Then
            assertThatThrownBy(() -> supporterService.getSupporters(999L, defaultPageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            assertSupporterQueriesNotCalled();
        }

        @Test
        @DisplayName("Should throw MessageException when userId is 0")
        void getSupporters_WhenUserIdIsZero_ShouldThrowMessageException() {
            assertThatThrownBy(() -> supporterService.getSupporters(0L, defaultPageable))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("ID must be a positive number");

            verify(userValidationService, never()).getUserById(any());
            assertSupporterQueriesNotCalled();
        }

        @Test
        @DisplayName("Should throw MessageException when userId is negative")
        void getSupporters_WhenUserIdIsNegative_ShouldThrowMessageException() {
            assertThatThrownBy(() -> supporterService.getSupporters(-5L, defaultPageable))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("ID must be a positive number");

            verify(userValidationService, never()).getUserById(any());
            assertSupporterQueriesNotCalled();
        }

        @Test
        @DisplayName("A sanctioned creator should give 404 to another USER")
        void getSupporters_SanctionedCreatorAsThirdParty_ShouldThrowResourceNotFoundException() {
            // Given
            givenCreatorExists();
            givenCaller(THIRD_PARTY_ID);
            when(roleValidationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);
            givenCreatorSanctioned();

            // When & Then
            assertThatThrownBy(() -> supporterService.getSupporters(CREATOR_ID, defaultPageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(donationValidationService, never()).validateSupporterSort(anyString(), anyBoolean());
            assertSupporterQueriesNotCalled();
        }

        @Test
        @DisplayName("A sanctioned creator should give 404 to an anonymous caller")
        void getSupporters_SanctionedCreatorAsAnonymous_ShouldThrowResourceNotFoundException() {
            // Given
            givenCreatorExists();
            givenAnonymousCaller();
            givenCreatorSanctioned();

            // When & Then
            assertThatThrownBy(() -> supporterService.getSupporters(CREATOR_ID, defaultPageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(donationValidationService, never()).validateSupporterSort(anyString(), anyBoolean());
            assertSupporterQueriesNotCalled();
        }

        @Test
        @DisplayName("A sanctioned creator should give 200 to an ADMIN, without checking the sanction")
        void getSupporters_SanctionedCreatorAsAdmin_ShouldReturnPage() {
            // Given
            creator.setBanned(true);
            givenCreatorExists();
            givenCaller(ADMIN_ID);
            when(roleValidationService.isAdmin(ADMIN_ID)).thenReturn(true);
            givenSupporters(List.of(rowA), 1L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID))).thenReturn(List.of(donorA));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTotalAmount()).isEqualByComparingTo("75.50");
            verify(userValidationService, never()).validateProfileNotSanctioned(any());
        }

        @Test
        @DisplayName("A sanctioned creator should give 200 to himself (not reachable through the API: the JWT filter answers 403)")
        void getSupporters_SanctionedCreatorAsHimself_ShouldReturnPage() {
            // Given
            creator.setBanned(true);
            givenCreatorExists();
            givenCaller(CREATOR_ID);
            givenSupporters(List.of(rowA), 1L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID))).thenReturn(List.of(donorA));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTotalAmount()).isEqualByComparingTo("75.50");
            verify(userValidationService, never()).validateProfileNotSanctioned(any());
        }

        private void givenCreatorSanctioned() {
            doThrow(new ResourceNotFoundException("User not found"))
                    .when(userValidationService).validateProfileNotSanctioned(creator);
        }
    }

    @Nested
    @DisplayName("Sort and pagination")
    class SortAndPaginationTests {

        @Test
        @DisplayName("Should call validateSupporterSort with the sortBy and fullView = true after the creator checks")
        void getSupporters_AsCreator_ShouldValidateSortWithFullViewAfterCreatorChecks() {
            // Given
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "lastDonationAt"));
            givenCreatorExists();
            givenCaller(CREATOR_ID);
            givenSupporters(List.of(), 0L);

            // When
            supporterService.getSupporters(CREATOR_ID, pageable);

            // Then
            InOrder inOrder = inOrder(userValidationService, authenticatedUserService, donationValidationService,
                    donationRepository);
            inOrder.verify(userValidationService).getUserById(CREATOR_ID);
            inOrder.verify(authenticatedUserService).findAuthenticatedUserId();
            inOrder.verify(donationValidationService).validateSupporterSort("lastDonationAt", true);
            inOrder.verify(donationRepository).findSupportersByCreatorId(CREATOR_ID, "lastDonationAt", true, 0L, 10);
        }

        @Test
        @DisplayName("Should call validateSupporterSort with fullView = false after validateProfileNotSanctioned")
        void getSupporters_AsThirdParty_ShouldValidateSortWithoutFullViewAfterBlockCheck() {
            // Given
            givenCreatorExists();
            givenCaller(THIRD_PARTY_ID);
            when(roleValidationService.isAdmin(THIRD_PARTY_ID)).thenReturn(false);
            givenSupporters(List.of(), 0L);

            // When
            supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            InOrder inOrder = inOrder(userValidationService, roleValidationService, donationValidationService,
                    donationRepository);
            inOrder.verify(userValidationService).getUserById(CREATOR_ID);
            inOrder.verify(roleValidationService).isAdmin(THIRD_PARTY_ID);
            inOrder.verify(userValidationService).validateProfileNotSanctioned(creator);
            inOrder.verify(donationValidationService).validateSupporterSort("totalAmount", false);
            inOrder.verify(donationRepository).findSupportersByCreatorId(CREATOR_ID, "totalAmount", true, 0L, 10);
        }

        @Test
        @DisplayName("Should not call the supporter queries when validateSupporterSort throws")
        void getSupporters_WhenSortValidationThrows_ShouldNotCallQueries() {
            // Given
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "lastDonationAt"));
            givenCreatorExists();
            givenAnonymousCaller();
            doThrow(new MessageException("Only the creator or an admin can sort supporters by lastDonationAt"))
                    .when(donationValidationService).validateSupporterSort("lastDonationAt", false);

            // When & Then
            assertThatThrownBy(() -> supporterService.getSupporters(CREATOR_ID, pageable))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Only the creator or an admin can sort supporters by lastDonationAt");

            assertSupporterQueriesNotCalled();
        }

        @Test
        @DisplayName("Should pass the sortBy, direction, offset and size of the Pageable and take totalElements from the count")
        void getSupporters_ShouldPassPageableToQueriesAndTakeTotalFromCount() {
            // Given
            Pageable pageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "lastDonationAt"));
            givenCreatorExists();
            givenCaller(CREATOR_ID);
            when(donationRepository.findSupportersByCreatorId(CREATOR_ID, "lastDonationAt", false, 10L, 5))
                    .thenReturn(List.of(rowA));
            when(donationRepository.countSupportersByCreatorId(CREATOR_ID)).thenReturn(11L);
            when(userRepository.findAllWithDetailsByIdIn(List.of(DONOR_A_ID))).thenReturn(List.of(donorA));

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, pageable);

            // Then
            verify(donationValidationService).validateSupporterSort("lastDonationAt", true);
            verify(donationRepository).findSupportersByCreatorId(CREATOR_ID, "lastDonationAt", false, 10L, 5);
            assertThat(result.getTotalElements()).isEqualTo(11L);
            assertThat(result.getNumber()).isEqualTo(2);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("An unsorted Pageable should use totalAmount and desc")
        void getSupporters_WithUnsortedPageable_ShouldUseTotalAmountDesc() {
            // Given
            Pageable pageable = PageRequest.of(1, 3);
            givenCreatorExists();
            givenAnonymousCaller();
            when(donationRepository.findSupportersByCreatorId(CREATOR_ID, "totalAmount", true, 3L, 3))
                    .thenReturn(List.of());
            when(donationRepository.countSupportersByCreatorId(CREATOR_ID)).thenReturn(2L);

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, pageable);

            // Then
            verify(donationValidationService).validateSupporterSort("totalAmount", false);
            verify(donationRepository).findSupportersByCreatorId(CREATOR_ID, "totalAmount", true, 3L, 3);
            assertThat(result.getTotalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Without donations should return an empty page and not call findAllWithDetailsByIdIn")
        void getSupporters_WithoutDonations_ShouldReturnEmptyPage() {
            // Given
            givenCreatorExists();
            givenAnonymousCaller();
            givenSupporters(List.of(), 0L);

            // When
            Page<SupporterResponseDTO> result = supporterService.getSupporters(CREATOR_ID, defaultPageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(userRepository, never()).findAllWithDetailsByIdIn(any());
        }
    }

    private void givenCreatorExists() {
        when(userValidationService.getUserById(CREATOR_ID)).thenReturn(creator);
    }

    private void givenCaller(Long callerId) {
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.of(callerId));
    }

    private void givenAnonymousCaller() {
        when(authenticatedUserService.findAuthenticatedUserId()).thenReturn(Optional.empty());
    }

    // Stubs both queries for any sort and page. The tests on the arguments verify them explicitly
    private void givenSupporters(List<DonationRepository.SupporterProjection> rows, long total) {
        when(donationRepository.findSupportersByCreatorId(anyLong(), anyString(), anyBoolean(), anyLong(), anyInt()))
                .thenReturn(rows);
        when(donationRepository.countSupportersByCreatorId(CREATOR_ID)).thenReturn(total);
    }

    private void assertSupporterQueriesNotCalled() {
        verify(donationRepository, never())
                .findSupportersByCreatorId(any(), anyString(), anyBoolean(), anyLong(), anyInt());
        verify(donationRepository, never()).countSupportersByCreatorId(any());
        verify(userRepository, never()).findAllWithDetailsByIdIn(any());
    }

    private static User userWithName(Long id, String userName) {
        User user = new User();
        user.setId(id);
        user.setUserInfo(UserInfo.builder().id(id * 100).userName(userName).build());
        return user;
    }

    private static DonationRepository.SupporterProjection supporterRow(Long donorUserId, BigDecimal totalAmount,
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
}
