package com.vaPaTi.vaPaTi.service.userblock;

import com.vaPaTi.vaPaTi.dtos.userblock.BlockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UnblockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UserBlockResponseDTO;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.entity.userblock.UserBlock;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.userblock.UserBlockMapper;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.repository.userblock.UserBlockRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.service.follower.FollowerService;
import com.vaPaTi.vaPaTi.validation.userblock.UserBlockValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBlockService Tests")
class UserBlockServiceTest {

    private static final Long CALLER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String OTHER_USER_NAME = "otheruser";
    private static final LocalDateTime BLOCKED_AT = LocalDateTime.of(2026, 10, 29, 10, 0);

    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private UserBlockValidationService userBlockValidationService;
    @Mock
    private FollowerService followerService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private UserRepository userRepository;
    @Spy
    private UserBlockMapper userBlockMapper = new UserBlockMapper();

    @InjectMocks
    private UserBlockService userBlockService;

    private User caller;
    private User otherUser;

    @BeforeEach
    void setUp() {
        caller = User.builder().id(CALLER_ID).build();
        otherUser = buildUser(OTHER_USER_ID, OTHER_USER_NAME);
    }

    @Nested
    @DisplayName("blockUser")
    class BlockUserTests {

        @Test
        @DisplayName("Should validate in order, save the block and then remove the follows, with the userName in the message")
        void blockUser_Valid_ShouldSaveThenRemoveFollows() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockValidationService.validateAndGetUserToBlock(OTHER_USER_ID)).thenReturn(otherUser);
            when(userRepository.getReferenceById(CALLER_ID)).thenReturn(caller);
            when(userBlockRepository.save(any(UserBlock.class))).thenAnswer(invocation -> {
                UserBlock block = invocation.getArgument(0);
                block.setId(100L);
                block.setCreatedAt(BLOCKED_AT);
                return block;
            });

            // When
            BlockResponseDTO response = userBlockService.blockUser(OTHER_USER_ID);

            // Then
            InOrder inOrder = inOrder(userBlockValidationService, userBlockRepository, followerService);
            inOrder.verify(userBlockValidationService).validateNotSelfBlock(CALLER_ID, OTHER_USER_ID);
            inOrder.verify(userBlockValidationService).validateAndGetUserToBlock(OTHER_USER_ID);
            inOrder.verify(userBlockValidationService).validateNotAlreadyBlocked(CALLER_ID, OTHER_USER_ID);
            inOrder.verify(userBlockRepository).save(any(UserBlock.class));
            inOrder.verify(followerService).removeFollowsBetween(CALLER_ID, OTHER_USER_ID);

            ArgumentCaptor<UserBlock> captor = ArgumentCaptor.forClass(UserBlock.class);
            verify(userBlockRepository).save(captor.capture());
            assertThat(captor.getValue().getBlocker()).isSameAs(caller);
            assertThat(captor.getValue().getBlockedUser()).isSameAs(otherUser);
            // The caller is linked by reference, without loading it
            verify(userRepository, never()).findById(any());

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Successfully blocked " + OTHER_USER_NAME);
            assertThat(response.getBlockedUser().getId()).isEqualTo(OTHER_USER_ID);
            assertThat(response.getBlockedUser().getUserName()).isEqualTo(OTHER_USER_NAME);
            assertThat(response.getBlockedUser().getBlockedAt()).isEqualTo(BLOCKED_AT);
        }

        @Test
        @DisplayName("Should throw MessageException on a self block, without loading the user, saving or removing follows")
        void blockUser_SelfBlock_ShouldNotSaveNorRemoveFollows() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            doThrow(new MessageException("Users cannot block themselves"))
                    .when(userBlockValidationService).validateNotSelfBlock(CALLER_ID, CALLER_ID);

            // When & Then
            assertThatThrownBy(() -> userBlockService.blockUser(CALLER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Users cannot block themselves");

            verify(userBlockValidationService, never()).validateAndGetUserToBlock(any());
            verify(userBlockValidationService, never()).validateNotAlreadyBlocked(any(), any());
            assertNothingWritten();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the user to block is not found, before the already blocked check")
        void blockUser_UserNotFound_ShouldNotCheckAlreadyBlockedNorWrite() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockValidationService.validateAndGetUserToBlock(OTHER_USER_ID))
                    .thenThrow(new ResourceNotFoundException("User to block not found with ID: " + OTHER_USER_ID));

            // When & Then
            assertThatThrownBy(() -> userBlockService.blockUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to block not found with ID: " + OTHER_USER_ID);

            verify(userBlockValidationService, never()).validateNotAlreadyBlocked(any(), any());
            assertNothingWritten();
        }

        @Test
        @DisplayName("Should throw ConflictException when the caller already blocks the user, without saving or removing follows")
        void blockUser_AlreadyBlocked_ShouldNotSaveNorRemoveFollows() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockValidationService.validateAndGetUserToBlock(OTHER_USER_ID)).thenReturn(otherUser);
            doThrow(new ConflictException("You have already blocked this user"))
                    .when(userBlockValidationService).validateNotAlreadyBlocked(CALLER_ID, OTHER_USER_ID);

            // When & Then
            assertThatThrownBy(() -> userBlockService.blockUser(OTHER_USER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("You have already blocked this user");

            assertNothingWritten();
        }

        @Test
        @DisplayName("Should propagate the unique constraint violation of a simultaneous duplicate without removing follows")
        void blockUser_UniqueViolationOnSave_ShouldNotRemoveFollows() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockValidationService.validateAndGetUserToBlock(OTHER_USER_ID)).thenReturn(otherUser);
            when(userRepository.getReferenceById(CALLER_ID)).thenReturn(caller);
            when(userBlockRepository.save(any(UserBlock.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_user_block_blocker_blocked"));

            // When & Then
            assertThatThrownBy(() -> userBlockService.blockUser(OTHER_USER_ID))
                    .isInstanceOf(DataIntegrityViolationException.class);

            verifyNoInteractions(followerService);
        }

        private void assertNothingWritten() {
            verify(userBlockRepository, never()).save(any());
            verifyNoInteractions(followerService);
        }
    }

    @Nested
    @DisplayName("unblockUser")
    class UnblockUserTests {

        @Test
        @DisplayName("Should delete the validated block and return the message and the id, without touching follower")
        void unblockUser_Valid_ShouldDeleteValidatedBlock() {
            // Given
            UserBlock block = UserBlock.builder().id(100L).blocker(caller).blockedUser(otherUser).build();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockValidationService.validateAndGetBlock(CALLER_ID, OTHER_USER_ID)).thenReturn(block);

            // When
            UnblockResponseDTO response = userBlockService.unblockUser(OTHER_USER_ID);

            // Then
            InOrder inOrder = inOrder(userBlockValidationService, userBlockRepository);
            inOrder.verify(userBlockValidationService).validateNotSelfUnblock(CALLER_ID, OTHER_USER_ID);
            inOrder.verify(userBlockValidationService).validateAndGetBlock(CALLER_ID, OTHER_USER_ID);
            inOrder.verify(userBlockRepository).delete(block);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("User successfully unblocked");
            assertThat(response.getUnblockedUserId()).isEqualTo(OTHER_USER_ID);
            verifyNoInteractions(followerService);
        }

        @Test
        @DisplayName("Should throw MessageException on a self unblock, without looking up the block or deleting")
        void unblockUser_SelfUnblock_ShouldNotDelete() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            doThrow(new MessageException("Users cannot unblock themselves"))
                    .when(userBlockValidationService).validateNotSelfUnblock(CALLER_ID, CALLER_ID);

            // When & Then
            assertThatThrownBy(() -> userBlockService.unblockUser(CALLER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("Users cannot unblock themselves");

            verify(userBlockValidationService, never()).validateAndGetBlock(any(), any());
            verify(userBlockRepository, never()).delete(any());
            verifyNoInteractions(followerService);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when the caller does not block the user, without deleting")
        void unblockUser_BlockNotFound_ShouldNotDelete() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockValidationService.validateAndGetBlock(CALLER_ID, OTHER_USER_ID))
                    .thenThrow(new ResourceNotFoundException("Block not found"));

            // When & Then
            assertThatThrownBy(() -> userBlockService.unblockUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Block not found");

            verify(userBlockRepository, never()).delete(any());
            verifyNoInteractions(followerService);
        }
    }

    @Nested
    @DisplayName("isBlocking")
    class IsBlockingTests {

        @Test
        @DisplayName("Should delegate to the validation with the id from the token")
        void isBlocking_ShouldDelegateWithTokenId() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockValidationService.isBlocking(CALLER_ID, OTHER_USER_ID)).thenReturn(true);

            // When & Then
            assertThat(userBlockService.isBlocking(OTHER_USER_ID)).isTrue();
            verify(userBlockValidationService).isBlocking(CALLER_ID, OTHER_USER_ID);
        }

        @Test
        @DisplayName("Should return false when the validation says the caller does not block the user")
        void isBlocking_NotBlocking_ShouldReturnFalse() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockValidationService.isBlocking(CALLER_ID, OTHER_USER_ID)).thenReturn(false);

            // When & Then
            assertThat(userBlockService.isBlocking(OTHER_USER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("getMyBlocks")
    class GetMyBlocksTests {

        @Test
        @DisplayName("Should query with the token id, a non-null now, the requested sort plus id DESC and the same page and size")
        void getMyBlocks_ShouldAddIdDescTieBreaker() {
            // Given
            Pageable pageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockRepository.findVisibleByBlockerId(eq(CALLER_ID), any(LocalDateTime.class), any(Pageable.class)))
                    .thenAnswer(invocation -> new PageImpl<UserBlock>(List.of(), invocation.getArgument(2), 10L));

            // When
            userBlockService.getMyBlocks(pageable);

            // Then
            ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userBlockRepository).findVisibleByBlockerId(eq(CALLER_ID), nowCaptor.capture(),
                    pageableCaptor.capture());
            assertThat(nowCaptor.getValue()).isNotNull();

            Pageable sent = pageableCaptor.getValue();
            assertThat(sent.getPageNumber()).isEqualTo(2);
            assertThat(sent.getPageSize()).isEqualTo(5);
            assertThat(sent.getSort().toList()).containsExactly(
                    Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }

        @Test
        @DisplayName("Should keep an ascending requested sort and still break ties by id DESC")
        void getMyBlocks_AscendingSort_ShouldKeepItAndAddIdDesc() {
            // Given
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockRepository.findVisibleByBlockerId(eq(CALLER_ID), any(LocalDateTime.class), any(Pageable.class)))
                    .thenAnswer(invocation -> new PageImpl<UserBlock>(List.of(), invocation.getArgument(2), 0L));

            // When
            userBlockService.getMyBlocks(pageable);

            // Then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userBlockRepository).findVisibleByBlockerId(eq(CALLER_ID), any(LocalDateTime.class),
                    pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getSort().toList()).containsExactly(
                    Sort.Order.asc("createdAt"), Sort.Order.desc("id"));
        }

        @Test
        @DisplayName("Should map each block to the blocked user DTO and keep totalElements")
        void getMyBlocks_ShouldMapRowsAndKeepTotal() {
            // Given
            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
            UserBlock block = UserBlock.builder()
                    .id(100L).blocker(caller).blockedUser(otherUser).createdAt(BLOCKED_AT).build();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CALLER_ID);
            when(userBlockRepository.findVisibleByBlockerId(eq(CALLER_ID), any(LocalDateTime.class), any(Pageable.class)))
                    .thenAnswer(invocation -> new PageImpl<>(List.of(block), invocation.getArgument(2), 2L));

            // When
            Page<UserBlockResponseDTO> result = userBlockService.getMyBlocks(pageable);

            // Then
            assertThat(result.getTotalElements()).isEqualTo(2L);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(1);
            UserBlockResponseDTO dto = result.getContent().get(0);
            assertThat(dto.getId()).isEqualTo(OTHER_USER_ID);
            assertThat(dto.getUserName()).isEqualTo(OTHER_USER_NAME);
            assertThat(dto.getBlockedAt()).isEqualTo(BLOCKED_AT);
        }
    }

    private static User buildUser(Long id, String userName) {
        User user = User.builder().id(id).build();
        user.setUserInfo(UserInfo.builder()
                .firstName("First")
                .lastName("Last")
                .userName(userName)
                .profilePicture("pic.png")
                .build());
        return user;
    }
}
