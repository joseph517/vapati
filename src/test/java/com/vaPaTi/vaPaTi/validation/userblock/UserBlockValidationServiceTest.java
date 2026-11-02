package com.vaPaTi.vaPaTi.validation.userblock;

import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.userblock.UserBlock;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.repository.userblock.UserBlockRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBlockValidationService tests")
class UserBlockValidationServiceTest {

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountStatusValidationService accountStatusValidationService;

    @InjectMocks
    private UserBlockValidationService userBlockValidationService;

    private Long currentUserId;
    private Long otherUserId;
    private User otherUser;

    @BeforeEach
    void setUp() {
        currentUserId = 1L;
        otherUserId = 2L;
        otherUser = User.builder().id(otherUserId).build();
    }

    @Nested
    @DisplayName("validateNoBlockBetween()")
    class NoBlockBetweenTests {

        @Test
        @DisplayName("Should pass when there is no block in either direction")
        void validateNoBlockBetween_WhenNoBlock_ShouldPass() {
            when(userBlockRepository.existsBlockBetween(currentUserId, otherUserId)).thenReturn(false);

            assertThatCode(() -> userBlockValidationService.validateNoBlockBetween(currentUserId, otherUserId))
                    .doesNotThrowAnyException();
            verify(userBlockRepository).existsBlockBetween(currentUserId, otherUserId);
        }

        @Test
        @DisplayName("Should throw 403 when there is a block between both users")
        void validateNoBlockBetween_WhenBlocked_ShouldThrowForbidden() {
            when(userBlockRepository.existsBlockBetween(currentUserId, otherUserId)).thenReturn(true);

            assertThatThrownBy(() -> userBlockValidationService.validateNoBlockBetween(currentUserId, otherUserId))
                    .isExactlyInstanceOf(ForbiddenActionException.class)
                    .hasMessage("You cannot interact with this user");
        }

        @Test
        @DisplayName("Should throw the same 403 when the block is in the other direction")
        void validateNoBlockBetween_WhenBlockedTheOtherWay_ShouldThrowSameForbidden() {
            when(userBlockRepository.existsBlockBetween(otherUserId, currentUserId)).thenReturn(true);

            assertThatThrownBy(() -> userBlockValidationService.validateNoBlockBetween(otherUserId, currentUserId))
                    .isExactlyInstanceOf(ForbiddenActionException.class)
                    .hasMessage("You cannot interact with this user");
        }

        @Test
        @DisplayName("Should pass without querying when the user ID is null")
        void validateNoBlockBetween_WhenUserIdNull_ShouldNotQuery() {
            assertThatCode(() -> userBlockValidationService.validateNoBlockBetween(null, otherUserId))
                    .doesNotThrowAnyException();
            verifyNoInteractions(userBlockRepository, userRepository, accountStatusValidationService);
        }

        @Test
        @DisplayName("Should pass without querying when the other user ID is null")
        void validateNoBlockBetween_WhenOtherUserIdNull_ShouldNotQuery() {
            assertThatCode(() -> userBlockValidationService.validateNoBlockBetween(currentUserId, null))
                    .doesNotThrowAnyException();
            verifyNoInteractions(userBlockRepository, userRepository, accountStatusValidationService);
        }

        @Test
        @DisplayName("Should pass without querying when both IDs are null")
        void validateNoBlockBetween_WhenBothIdsNull_ShouldNotQuery() {
            assertThatCode(() -> userBlockValidationService.validateNoBlockBetween(null, null))
                    .doesNotThrowAnyException();
            verifyNoInteractions(userBlockRepository, userRepository, accountStatusValidationService);
        }
    }

    @Nested
    @DisplayName("validateNotSelfBlock() / validateNotSelfUnblock()")
    class SelfTests {

        @Test
        @DisplayName("Should throw 400 when the user blocks themselves")
        void validateNotSelfBlock_WhenSameId_ShouldThrow() {
            assertThatThrownBy(() -> userBlockValidationService.validateNotSelfBlock(currentUserId, 1L))
                    .isExactlyInstanceOf(MessageException.class)
                    .hasMessage("Users cannot block themselves");
            verifyNoInteractions(userBlockRepository, userRepository);
        }

        @Test
        @DisplayName("Should pass when blocking another user")
        void validateNotSelfBlock_WhenDifferentId_ShouldPass() {
            assertThatCode(() -> userBlockValidationService.validateNotSelfBlock(currentUserId, otherUserId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw 400 when the user unblocks themselves")
        void validateNotSelfUnblock_WhenSameId_ShouldThrow() {
            assertThatThrownBy(() -> userBlockValidationService.validateNotSelfUnblock(currentUserId, 1L))
                    .isExactlyInstanceOf(MessageException.class)
                    .hasMessage("Users cannot unblock themselves");
            verifyNoInteractions(userBlockRepository, userRepository);
        }

        @Test
        @DisplayName("Should pass when unblocking another user")
        void validateNotSelfUnblock_WhenDifferentId_ShouldPass() {
            assertThatCode(() -> userBlockValidationService.validateNotSelfUnblock(currentUserId, otherUserId))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateAndGetUserToBlock()")
    class UserToBlockTests {

        @Test
        @DisplayName("Should return the user when it exists and is not sanctioned")
        void validateAndGetUserToBlock_WhenActive_ShouldReturnUser() {
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
            when(accountStatusValidationService.isSanctioned(otherUser)).thenReturn(false);

            assertThat(userBlockValidationService.validateAndGetUserToBlock(otherUserId)).isSameAs(otherUser);
        }

        @Test
        @DisplayName("Should throw 404 when the user does not exist or is deleted")
        void validateAndGetUserToBlock_WhenNotFound_ShouldThrow() {
            Long nonExistentUserId = 999L;
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBlockValidationService.validateAndGetUserToBlock(nonExistentUserId))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to block not found with ID: " + nonExistentUserId);
            verifyNoInteractions(accountStatusValidationService);
        }

        @Test
        @DisplayName("Should throw the same 404 when the user is banned")
        void validateAndGetUserToBlock_WhenBanned_ShouldThrow() {
            User bannedUser = User.builder().id(otherUserId).banned(true).build();
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(bannedUser));
            when(accountStatusValidationService.isSanctioned(bannedUser)).thenReturn(true);

            assertThatThrownBy(() -> userBlockValidationService.validateAndGetUserToBlock(otherUserId))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to block not found with ID: " + otherUserId);
        }

        @Test
        @DisplayName("Should throw the same 404 when the user has a current suspension")
        void validateAndGetUserToBlock_WhenSuspended_ShouldThrow() {
            User suspendedUser = User.builder().id(otherUserId)
                    .suspendedUntil(LocalDateTime.now().plusDays(1)).build();
            when(userRepository.findById(otherUserId)).thenReturn(Optional.of(suspendedUser));
            when(accountStatusValidationService.isSanctioned(suspendedUser)).thenReturn(true);

            assertThatThrownBy(() -> userBlockValidationService.validateAndGetUserToBlock(otherUserId))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User to block not found with ID: " + otherUserId);
        }
    }

    @Nested
    @DisplayName("validateNotAlreadyBlocked()")
    class AlreadyBlockedTests {

        @Test
        @DisplayName("Should throw 409 when the current user already blocks the other user")
        void validateNotAlreadyBlocked_WhenAlreadyBlocked_ShouldThrowConflict() {
            when(userBlockRepository.existsByBlockerIdAndBlockedUserId(currentUserId, otherUserId)).thenReturn(true);

            assertThatThrownBy(() -> userBlockValidationService.validateNotAlreadyBlocked(currentUserId, otherUserId))
                    .isExactlyInstanceOf(ConflictException.class)
                    .hasMessage("You have already blocked this user");
        }

        @Test
        @DisplayName("Should pass and only look at its own direction, not at a block by the other user")
        void validateNotAlreadyBlocked_WhenNotBlocked_ShouldOnlyCheckOwnDirection() {
            when(userBlockRepository.existsByBlockerIdAndBlockedUserId(currentUserId, otherUserId)).thenReturn(false);

            assertThatCode(() -> userBlockValidationService.validateNotAlreadyBlocked(currentUserId, otherUserId))
                    .doesNotThrowAnyException();
            verify(userBlockRepository).existsByBlockerIdAndBlockedUserId(currentUserId, otherUserId);
            verify(userBlockRepository, never()).existsByBlockerIdAndBlockedUserId(otherUserId, currentUserId);
            verify(userBlockRepository, never()).existsBlockBetween(any(), any());
            verifyNoMoreInteractions(userBlockRepository);
        }
    }

    @Nested
    @DisplayName("validateAndGetBlock()")
    class BlockRowTests {

        @Test
        @DisplayName("Should return the row without loading or validating the blocked user")
        void validateAndGetBlock_WhenExists_ShouldReturnRowWithoutLoadingUser() {
            UserBlock block = UserBlock.builder().id(10L).build();
            when(userBlockRepository.findByBlockerIdAndBlockedUserId(currentUserId, otherUserId))
                    .thenReturn(Optional.of(block));

            assertThat(userBlockValidationService.validateAndGetBlock(currentUserId, otherUserId)).isSameAs(block);
            verifyNoInteractions(userRepository, accountStatusValidationService);
        }

        @Test
        @DisplayName("Should throw 404 when there is no row")
        void validateAndGetBlock_WhenMissing_ShouldThrow() {
            when(userBlockRepository.findByBlockerIdAndBlockedUserId(currentUserId, otherUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userBlockValidationService.validateAndGetBlock(currentUserId, otherUserId))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Block not found");
            verifyNoInteractions(userRepository, accountStatusValidationService);
        }
    }

    @Nested
    @DisplayName("isBlocking()")
    class IsBlockingTests {

        @Test
        @DisplayName("Should return false with the same ID, without querying")
        void isBlocking_WhenSameId_ShouldReturnFalseWithoutQuerying() {
            assertThat(userBlockValidationService.isBlocking(currentUserId, 1L)).isFalse();
            verifyNoInteractions(userBlockRepository, userRepository, accountStatusValidationService);
        }

        @Test
        @DisplayName("Should return true when the current user blocks the other user")
        void isBlocking_WhenBlocking_ShouldReturnTrue() {
            when(userBlockRepository.existsByBlockerIdAndBlockedUserId(currentUserId, otherUserId)).thenReturn(true);

            assertThat(userBlockValidationService.isBlocking(currentUserId, otherUserId)).isTrue();
            verifyNoInteractions(userRepository, accountStatusValidationService);
        }

        @Test
        @DisplayName("Should return false when there is no row in that direction, without loading the user")
        void isBlocking_WhenNotBlocking_ShouldReturnFalse() {
            when(userBlockRepository.existsByBlockerIdAndBlockedUserId(currentUserId, otherUserId)).thenReturn(false);

            assertThat(userBlockValidationService.isBlocking(currentUserId, otherUserId)).isFalse();
            verify(userBlockRepository, never()).existsBlockBetween(any(), any());
            verifyNoInteractions(userRepository, accountStatusValidationService);
        }
    }
}
