package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CreateVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.ProcessVerificationRequestDTO;
import com.vaPaTi.vaPaTi.dtos.VerificationStatusResponseDTO;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.VerificationRequest;
import com.vaPaTi.vaPaTi.validation.VerificationStatus;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.mapper.VerificationRequestMapper;
import com.vaPaTi.vaPaTi.repository.VerificationRequestRepository;
import com.vaPaTi.vaPaTi.validation.VerificationRequestValitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationRequestService Tests")
class VerificationRequestServiceTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;
    @Mock
    private VerificationRequestMapper verificationRequestMapper;
    @Mock
    private VerificationRequestValitation verificationRequestValitation;

    @InjectMocks
    private VerificationRequestService verificationRequestService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_REQUEST_ID = 1L;
    private static final String PENDING_STATUS = "PENDING";
    private static final String APPROVED_STATUS = "APPROVED";
    private static final String REJECTED_STATUS = "REJECTED";

    private User testUser;
    private VerificationRequest pendingRequest;
    private VerificationRequest rejectedRequest;
    private VerificationRequest approvedRequest;
    private CreateVerificationRequestDTO createDTO;
    private ProcessVerificationRequestDTO processDTO;
    private VerificationStatusResponseDTO statusResponseDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setVerified(false);

        pendingRequest = new VerificationRequest();
        pendingRequest.setId(TEST_REQUEST_ID);
        pendingRequest.setUser(testUser);
        pendingRequest.setStatus(PENDING_STATUS);
        pendingRequest.setCreatedAt(LocalDateTime.now());

        rejectedRequest = new VerificationRequest();
        rejectedRequest.setId(TEST_REQUEST_ID);
        rejectedRequest.setUser(testUser);
        rejectedRequest.setStatus(REJECTED_STATUS);
        rejectedRequest.setCreatedAt(LocalDateTime.now());

        approvedRequest = new VerificationRequest();
        approvedRequest.setId(TEST_REQUEST_ID);
        approvedRequest.setUser(testUser);
        approvedRequest.setStatus(APPROVED_STATUS);
        approvedRequest.setCreatedAt(LocalDateTime.now());

        createDTO = new CreateVerificationRequestDTO();
        createDTO.setUserId(TEST_USER_ID);
        createDTO.setDniFront("front_image_url");
        createDTO.setDniBack("back_image_url");
        createDTO.setSelfieUser("selfie_image_url");

        processDTO = new ProcessVerificationRequestDTO();
        processDTO.setRequestId(TEST_REQUEST_ID);
        processDTO.setStatus(VerificationStatus.APPROVED);

        statusResponseDTO = new VerificationStatusResponseDTO();
        statusResponseDTO.setRequestStatus(VerificationStatus.PENDING);
        statusResponseDTO.setVerified(false);
    }

    @Nested
    @DisplayName("createVerificationRequest() tests")
    class CreateVerificationRequestTests {

        @Test
        @DisplayName("Should create new verification request successfully")
        void createVerificationRequest_WithNewRequest_ShouldCreateNewRequest() {
            // Given
            VerificationRequest newRequest = new VerificationRequest();
            newRequest.setId(TEST_REQUEST_ID);

            when(verificationRequestValitation.validateAndGetUser(TEST_USER_ID)).thenReturn(testUser);
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
            when(verificationRequestMapper.toEntity(createDTO, testUser)).thenReturn(newRequest);
            when(verificationRequestRepository.save(newRequest)).thenReturn(newRequest);

            // When
            Long result = verificationRequestService.createVerificationRequest(createDTO);

            // Then
            assertThat(result).isEqualTo(TEST_REQUEST_ID);

            verify(verificationRequestValitation).validateAndGetUser(TEST_USER_ID);
            verify(verificationRequestValitation).validateUserNotVerified(testUser);
            verify(verificationRequestRepository).findByUserId(TEST_USER_ID);
            verify(verificationRequestMapper).toEntity(createDTO, testUser);
            verify(verificationRequestRepository).save(newRequest);
        }

        @Test
        @DisplayName("Should update existing rejected request instead of creating new one")
        void createVerificationRequest_WithRejectedRequest_ShouldUpdateExistingRequest() {
            // Given
            when(verificationRequestValitation.validateAndGetUser(TEST_USER_ID)).thenReturn(testUser);
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(rejectedRequest));
            when(verificationRequestValitation.isPendingRequest(rejectedRequest)).thenReturn(false);
            when(verificationRequestValitation.isRejectedRequest(rejectedRequest)).thenReturn(true);
            when(verificationRequestRepository.save(rejectedRequest)).thenReturn(rejectedRequest);

            // When
            Long result = verificationRequestService.createVerificationRequest(createDTO);

            // Then
            assertThat(result).isEqualTo(TEST_REQUEST_ID);

            verify(verificationRequestMapper).updateFromDto(rejectedRequest, createDTO);
            verify(verificationRequestRepository).save(rejectedRequest);
            verify(verificationRequestMapper, never()).toEntity(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when user already has pending request")
        void createVerificationRequest_WithPendingRequest_ShouldThrowException() {
            // Given
            when(verificationRequestValitation.validateAndGetUser(TEST_USER_ID)).thenReturn(testUser);
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(pendingRequest));
            when(verificationRequestValitation.isPendingRequest(pendingRequest)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> verificationRequestService.createVerificationRequest(createDTO))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("There is already a pending request for this user.");

            verify(verificationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should validate user first before checking existing requests")
        void createVerificationRequest_ShouldValidateUserFirst() {
            // Given
            when(verificationRequestValitation.validateAndGetUser(TEST_USER_ID)).thenReturn(testUser);
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
            when(verificationRequestMapper.toEntity(any(), any())).thenReturn(pendingRequest);
            when(verificationRequestRepository.save(any())).thenReturn(pendingRequest);

            InOrder inOrder = inOrder(verificationRequestValitation, verificationRequestRepository);

            // When
            verificationRequestService.createVerificationRequest(createDTO);

            // Then
            inOrder.verify(verificationRequestValitation).validateAndGetUser(TEST_USER_ID);
            inOrder.verify(verificationRequestValitation).validateUserNotVerified(testUser);
            inOrder.verify(verificationRequestRepository).findByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should check user is not verified before creating request")
        void createVerificationRequest_ShouldCheckUserNotVerified() {
            // Given
            when(verificationRequestValitation.validateAndGetUser(TEST_USER_ID)).thenReturn(testUser);
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
            when(verificationRequestMapper.toEntity(any(), any())).thenReturn(pendingRequest);
            when(verificationRequestRepository.save(any())).thenReturn(pendingRequest);

            // When
            verificationRequestService.createVerificationRequest(createDTO);

            // Then
            verify(verificationRequestValitation).validateUserNotVerified(testUser);
        }

        @Test
        @DisplayName("Should create new request when existing request is approved")
        void createVerificationRequest_WithApprovedRequest_ShouldCreateNewRequest() {
            // Given
            VerificationRequest newRequest = new VerificationRequest();
            newRequest.setId(2L);

            when(verificationRequestValitation.validateAndGetUser(TEST_USER_ID)).thenReturn(testUser);
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(approvedRequest));
            when(verificationRequestValitation.isPendingRequest(approvedRequest)).thenReturn(false);
            when(verificationRequestValitation.isRejectedRequest(approvedRequest)).thenReturn(false);
            when(verificationRequestMapper.toEntity(createDTO, testUser)).thenReturn(newRequest);
            when(verificationRequestRepository.save(newRequest)).thenReturn(newRequest);

            // When
            Long result = verificationRequestService.createVerificationRequest(createDTO);

            // Then
            assertThat(result).isEqualTo(2L);

            verify(verificationRequestMapper).toEntity(createDTO, testUser);
            verify(verificationRequestRepository).save(newRequest);
            verify(verificationRequestMapper, never()).updateFromDto(any(), any());
        }
    }

    @Nested
    @DisplayName("processVerificationRequest() tests")
    class ProcessVerificationRequestTests {

        @Test
        @DisplayName("Should approve verification request and update user")
        void processVerificationRequest_WithApproval_ShouldApproveAndUpdateUser() {
            // Given
            processDTO.setStatus(VerificationStatus.APPROVED);
            when(verificationRequestValitation.findVerificationRequestById(TEST_REQUEST_ID)).thenReturn(pendingRequest);
            when(verificationRequestValitation.isApproved(VerificationStatus.APPROVED)).thenReturn(true);
            when(verificationRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

            // When
            VerificationRequest result = verificationRequestService.processVerificationRequest(processDTO);

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(request -> {
                        assertThat(request.getStatus()).isEqualTo(APPROVED_STATUS);
                        assertThat(request.getUpdatedAt()).isNotNull();
                    });

            verify(verificationRequestValitation).approveUserVerification(testUser);
            verify(verificationRequestRepository).save(pendingRequest);
        }

        @Test
        @DisplayName("Should reject verification request without updating user")
        void processVerificationRequest_WithRejection_ShouldRejectWithoutApprovingUser() {
            // Given
            processDTO.setStatus(VerificationStatus.REJECTED);
            when(verificationRequestValitation.findVerificationRequestById(TEST_REQUEST_ID)).thenReturn(pendingRequest);
            when(verificationRequestValitation.isApproved(VerificationStatus.REJECTED)).thenReturn(false);
            when(verificationRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

            // When
            VerificationRequest result = verificationRequestService.processVerificationRequest(processDTO);

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(request -> {
                        assertThat(request.getStatus()).isEqualTo(REJECTED_STATUS);
                        assertThat(request.getUpdatedAt()).isNotNull();
                    });

            verify(verificationRequestValitation, never()).approveUserVerification(any());
            verify(verificationRequestRepository).save(pendingRequest);
        }

        @Test
        @DisplayName("Should set updatedAt timestamp when processing request")
        void processVerificationRequest_WithApproval_ShouldSetUpdatedAt() {
            // Given
            processDTO.setStatus(VerificationStatus.APPROVED);
            LocalDateTime before = LocalDateTime.now();

            when(verificationRequestValitation.findVerificationRequestById(TEST_REQUEST_ID)).thenReturn(pendingRequest);
            when(verificationRequestValitation.isApproved(VerificationStatus.APPROVED)).thenReturn(true);
            when(verificationRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

            // When
            VerificationRequest result = verificationRequestService.processVerificationRequest(processDTO);

            // Then
            assertThat(result.getUpdatedAt())
                    .isNotNull()
                    .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
                    .isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("Should find verification request before processing")
        void processVerificationRequest_ShouldFindRequestFirst() {
            // Given
            processDTO.setStatus(VerificationStatus.APPROVED);
            when(verificationRequestValitation.findVerificationRequestById(TEST_REQUEST_ID)).thenReturn(pendingRequest);
            when(verificationRequestValitation.isApproved(any())).thenReturn(true);
            when(verificationRequestRepository.save(any())).thenReturn(pendingRequest);

            // When
            verificationRequestService.processVerificationRequest(processDTO);

            // Then
            verify(verificationRequestValitation).findVerificationRequestById(TEST_REQUEST_ID);
        }

        @Test
        @DisplayName("Should update status from DTO")
        void processVerificationRequest_ShouldUpdateStatusFromDTO() {
            // Given
            processDTO.setStatus(VerificationStatus.APPROVED);
            when(verificationRequestValitation.findVerificationRequestById(TEST_REQUEST_ID)).thenReturn(pendingRequest);
            when(verificationRequestValitation.isApproved(VerificationStatus.APPROVED)).thenReturn(true);
            when(verificationRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

            // When
            verificationRequestService.processVerificationRequest(processDTO);

            // Then
            assertThat(pendingRequest.getStatus()).isEqualTo(VerificationStatus.APPROVED.name());
        }

        @Test
        @DisplayName("Should check if status is approved before updating user")
        void processVerificationRequest_ShouldCheckIfApproved() {
            // Given
            processDTO.setStatus(VerificationStatus.APPROVED);
            when(verificationRequestValitation.findVerificationRequestById(TEST_REQUEST_ID)).thenReturn(pendingRequest);
            when(verificationRequestValitation.isApproved(VerificationStatus.APPROVED)).thenReturn(true);
            when(verificationRequestRepository.save(any())).thenReturn(pendingRequest);

            // When
            verificationRequestService.processVerificationRequest(processDTO);

            // Then
            verify(verificationRequestValitation).isApproved(VerificationStatus.APPROVED);
        }

        @Test
        @DisplayName("Should only approve user when status is approved")
        void processVerificationRequest_WithApproval_ShouldCallApproveUser() {
            // Given
            processDTO.setStatus(VerificationStatus.APPROVED);
            when(verificationRequestValitation.findVerificationRequestById(TEST_REQUEST_ID)).thenReturn(pendingRequest);
            when(verificationRequestValitation.isApproved(VerificationStatus.APPROVED)).thenReturn(true);
            when(verificationRequestRepository.save(any())).thenReturn(pendingRequest);

            // When
            verificationRequestService.processVerificationRequest(processDTO);

            // Then
            verify(verificationRequestValitation).approveUserVerification(testUser);
        }

        @Test
        @DisplayName("Should not approve user when status is not approved")
        void processVerificationRequest_WithNonApproval_ShouldNotCallApproveUser() {
            // Given
            processDTO.setStatus(VerificationStatus.PENDING);
            when(verificationRequestValitation.findVerificationRequestById(TEST_REQUEST_ID)).thenReturn(pendingRequest);
            when(verificationRequestValitation.isApproved(VerificationStatus.PENDING)).thenReturn(false);
            when(verificationRequestRepository.save(any())).thenReturn(pendingRequest);

            // When
            verificationRequestService.processVerificationRequest(processDTO);

            // Then
            verify(verificationRequestValitation, never()).approveUserVerification(any());
        }
    }

    @Nested
    @DisplayName("getVerificationStatus() tests")
    class GetVerificationStatusTests {

        @Test
        @DisplayName("Should return verification status DTO when request exists")
        void getVerificationStatus_WithExistingRequest_ShouldReturnStatusDTO() {
            // Given
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(pendingRequest));
            when(verificationRequestMapper.toStatusDTO(pendingRequest)).thenReturn(statusResponseDTO);

            // When
            VerificationStatusResponseDTO result = verificationRequestService.getVerificationStatus(TEST_USER_ID);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(statusResponseDTO);

            verify(verificationRequestRepository).findByUserId(TEST_USER_ID);
            verify(verificationRequestMapper).toStatusDTO(pendingRequest);
        }

        @Test
        @DisplayName("Should throw exception when no verification request found")
        void getVerificationStatus_WithNoRequest_ShouldThrowException() {
            // Given
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> verificationRequestService.getVerificationStatus(TEST_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("No verification request found.");

            verify(verificationRequestMapper, never()).toStatusDTO(any());
        }

        @Test
        @DisplayName("Should use mapper to create status DTO")
        void getVerificationStatus_ShouldUseMapper() {
            // Given
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(pendingRequest));
            when(verificationRequestMapper.toStatusDTO(pendingRequest)).thenReturn(statusResponseDTO);

            // When
            verificationRequestService.getVerificationStatus(TEST_USER_ID);

            // Then
            verify(verificationRequestMapper).toStatusDTO(pendingRequest);
        }

        @Test
        @DisplayName("Should query repository by user ID")
        void getVerificationStatus_ShouldQueryByUserId() {
            // Given
            when(verificationRequestRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(pendingRequest));
            when(verificationRequestMapper.toStatusDTO(any())).thenReturn(statusResponseDTO);

            // When
            verificationRequestService.getVerificationStatus(TEST_USER_ID);

            // Then
            verify(verificationRequestRepository).findByUserId(TEST_USER_ID);
        }
    }
}
