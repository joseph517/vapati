package com.vaPaTi.vaPaTi.service.follower;

import com.vaPaTi.vaPaTi.dtos.follower.FollowResponseDTO;
import com.vaPaTi.vaPaTi.dtos.follower.FollowerUserDTO;
import com.vaPaTi.vaPaTi.dtos.follower.FollowersListResponseDTO;
import com.vaPaTi.vaPaTi.dtos.follower.UnfollowResponseDTO;
import com.vaPaTi.vaPaTi.entity.follower.Follower;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.follower.FollowerMapper;
import com.vaPaTi.vaPaTi.repository.follower.FollowerRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.follower.FollowerValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowerService Tests")
class FollowerServiceTest {

    @Mock
    private FollowerRepository followerRepository;
    @Mock
    private FollowerMapper followerMapper;
    @Mock
    private FollowerValidationService followerValidationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private FollowerService followerService;

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String USER_NOT_FOUND = "User not found with ID: ";
    private static final String CURRENT_USER_NOT_FOUND = "Current user not found with ID: ";
    private static final String USER_TO_FOLLOW_NOT_FOUND = "User to follow not found with ID: ";
    private static final String USER_TO_UNFOLLOW_NOT_FOUND = "User to unfollow not found with ID: ";
    private static final String SELF_FOLLOW_ERROR = "Users cannot follow themselves";
    private static final String SELF_UNFOLLOW_ERROR = "Users cannot unfollow themselves";
    private static final String ALREADY_FOLLOWING_ERROR = "User is already being followed";
    private static final String FOLLOW_RELATION_NOT_FOUND = "Follow relationship not found";

    private User currentUser;
    private User userToFollow;
    private UserInfo currentUserInfo;
    private UserInfo otherUserInfo;
    private Follower followerRelationship;
    private FollowResponseDTO followResponseDto;
    private UnfollowResponseDTO unfollowResponseDto;
    private FollowersListResponseDTO followersListResponseDto;

    @BeforeEach
    void setUp() {
        currentUserInfo = new UserInfo();
        currentUserInfo.setUserName("current_user");

        otherUserInfo = new UserInfo();
        otherUserInfo.setUserName("other_user");

        currentUser = new User();
        currentUser.setId(CURRENT_USER_ID);
        currentUser.setUserInfo(currentUserInfo);

        userToFollow = new User();
        userToFollow.setId(OTHER_USER_ID);
        userToFollow.setUserInfo(otherUserInfo);

        followerRelationship = new Follower();
        followerRelationship.setId(1L);
        followerRelationship.setUser(userToFollow);
        followerRelationship.setFollower(currentUser);
        followerRelationship.setCreatedAt(LocalDateTime.now());

        FollowerUserDTO mockFollowerUser = new FollowerUserDTO(
                OTHER_USER_ID,
                "John",
                "Doe",
                "other_user",
                null,
                LocalDateTime.now()
        );

        followResponseDto = new FollowResponseDTO(
                "Successfully started following other_user",
                true,
                mockFollowerUser
        );

        unfollowResponseDto = new UnfollowResponseDTO(
                "Successfully unfollowed other_user",
                true,
                OTHER_USER_ID
        );

        followersListResponseDto = new FollowersListResponseDTO(
                CURRENT_USER_ID,
                0,
                List.of()
        );
    }

    @Nested
    @DisplayName("followUser() tests")
    class FollowUserTests {

        @Test
        @DisplayName("Should create follower relationship successfully with valid users")
        void followUser_WithValidUser_ShouldCreateFollowerRelationship() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerRepository.save(any(Follower.class))).thenReturn(followerRelationship);
            when(followerMapper.toFollowResponseDto(any(Follower.class), anyString())).thenReturn(followResponseDto);

            // When
            FollowResponseDTO result = followerService.followUser(OTHER_USER_ID);

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(response -> {
                        assertThat(response.getFollowedUser().getId()).isEqualTo(OTHER_USER_ID);
                        assertThat(response.isSuccess()).isTrue();
                        assertThat(response.getMessage()).contains("other_user");
                    });

            verify(followerRepository).save(any(Follower.class));
        }

        @Test
        @DisplayName("Should run the validations in order before saving the relationship")
        void followUser_ShouldValidateInOrderBeforeSaving() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerRepository.save(any(Follower.class))).thenReturn(followerRelationship);
            when(followerMapper.toFollowResponseDto(any(Follower.class), anyString())).thenReturn(followResponseDto);

            // When
            followerService.followUser(OTHER_USER_ID);

            // Then
            InOrder inOrder = inOrder(authenticatedUserService, followerValidationService, followerRepository, followerMapper);
            inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
            inOrder.verify(followerValidationService).validateNotSelfFollow(CURRENT_USER_ID, OTHER_USER_ID);
            inOrder.verify(followerValidationService).validateAndGetCurrentUser(CURRENT_USER_ID);
            inOrder.verify(followerValidationService).validateAndGetUserToFollow(OTHER_USER_ID);
            inOrder.verify(followerValidationService).validateNotAlreadyFollowing(userToFollow, currentUser);
            inOrder.verify(followerRepository).save(any(Follower.class));
            inOrder.verify(followerMapper).toFollowResponseDto(any(Follower.class), anyString());
        }

        @Test
        @DisplayName("Should throw exception when user tries to follow themselves")
        void followUser_WithSameUserId_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            doThrow(new MessageException(SELF_FOLLOW_ERROR))
                    .when(followerValidationService).validateNotSelfFollow(CURRENT_USER_ID, CURRENT_USER_ID);

            // When & Then
            assertThatThrownBy(() -> followerService.followUser(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(SELF_FOLLOW_ERROR);

            verify(followerValidationService, never()).validateAndGetCurrentUser(any());
            verify(followerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when current user is not found")
        void followUser_WithNonExistentCurrentUser_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(CURRENT_USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.followUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(CURRENT_USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerValidationService, never()).validateAndGetUserToFollow(any());
            verify(followerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user to follow is not found")
        void followUser_WithNonExistentUserToFollow_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToFollow(OTHER_USER_ID))
                    .thenThrow(new ResourceNotFoundException(USER_TO_FOLLOW_NOT_FOUND + OTHER_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.followUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(USER_TO_FOLLOW_NOT_FOUND + OTHER_USER_ID);

            verify(followerValidationService, never()).validateNotAlreadyFollowing(any(), any());
            verify(followerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when already following user")
        void followUser_WhenAlreadyFollowing_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            doThrow(new ConflictException(ALREADY_FOLLOWING_ERROR))
                    .when(followerValidationService).validateNotAlreadyFollowing(userToFollow, currentUser);

            // When & Then
            assertThatThrownBy(() -> followerService.followUser(OTHER_USER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage(ALREADY_FOLLOWING_ERROR);

            verify(followerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set correct follower relationship direction")
        void followUser_ShouldSetCorrectFollowerRelationship() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerRepository.save(any(Follower.class))).thenAnswer(invocation -> {
                Follower saved = invocation.getArgument(0);
                assertThat(saved.getUser()).isEqualTo(userToFollow);
                assertThat(saved.getFollower()).isEqualTo(currentUser);
                return saved;
            });
            when(followerMapper.toFollowResponseDto(any(), anyString())).thenReturn(followResponseDto);

            // When
            followerService.followUser(OTHER_USER_ID);

            // Then
            verify(followerRepository).save(any(Follower.class));
        }

        @Test
        @DisplayName("Should set createdAt timestamp when creating relationship")
        void followUser_ShouldSetCreatedAtTimestamp() {
            // Given
            LocalDateTime before = LocalDateTime.now();
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerRepository.save(any(Follower.class))).thenAnswer(invocation -> {
                Follower saved = invocation.getArgument(0);
                assertThat(saved.getCreatedAt())
                        .isNotNull()
                        .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
                        .isAfterOrEqualTo(before);
                return saved;
            });
            when(followerMapper.toFollowResponseDto(any(), anyString())).thenReturn(followResponseDto);

            // When
            followerService.followUser(OTHER_USER_ID);

            // Then
            verify(followerRepository).save(any(Follower.class));
        }

        @Test
        @DisplayName("Should include username in success message")
        void followUser_ShouldIncludeUsernameInMessage() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerRepository.save(any(Follower.class))).thenReturn(followerRelationship);
            when(followerMapper.toFollowResponseDto(any(Follower.class), contains("other_user")))
                    .thenReturn(followResponseDto);

            // When
            followerService.followUser(OTHER_USER_ID);

            // Then
            verify(followerMapper).toFollowResponseDto(any(Follower.class), contains("other_user"));
        }
    }

    @Nested
    @DisplayName("unfollowUser() tests")
    class UnfollowUserTests {

        @Test
        @DisplayName("Should delete follower relationship successfully")
        void unfollowUser_WithValidUser_ShouldDeleteFollowerRelationship() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToUnfollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerValidationService.validateAndGetFollowRelation(userToFollow, currentUser))
                    .thenReturn(followerRelationship);
            when(followerMapper.toUnfollowResponseDto(anyLong(), anyString())).thenReturn(unfollowResponseDto);

            // When
            UnfollowResponseDTO result = followerService.unfollowUser(OTHER_USER_ID);

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(response -> {
                        assertThat(response.getUnfollowedUserId()).isEqualTo(OTHER_USER_ID);
                        assertThat(response.isSuccess()).isTrue();
                        assertThat(response.getMessage()).contains("other_user");
                    });

            verify(followerRepository).delete(followerRelationship);
        }

        @Test
        @DisplayName("Should run the validations in order before deleting the relationship")
        void unfollowUser_ShouldValidateInOrderBeforeDeleting() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToUnfollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerValidationService.validateAndGetFollowRelation(userToFollow, currentUser))
                    .thenReturn(followerRelationship);
            when(followerMapper.toUnfollowResponseDto(anyLong(), anyString())).thenReturn(unfollowResponseDto);

            // When
            followerService.unfollowUser(OTHER_USER_ID);

            // Then
            InOrder inOrder = inOrder(authenticatedUserService, followerValidationService, followerRepository, followerMapper);
            inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
            inOrder.verify(followerValidationService).validateNotSelfUnfollow(CURRENT_USER_ID, OTHER_USER_ID);
            inOrder.verify(followerValidationService).validateAndGetCurrentUser(CURRENT_USER_ID);
            inOrder.verify(followerValidationService).validateAndGetUserToUnfollow(OTHER_USER_ID);
            inOrder.verify(followerValidationService).validateAndGetFollowRelation(userToFollow, currentUser);
            inOrder.verify(followerRepository).delete(followerRelationship);
            inOrder.verify(followerMapper).toUnfollowResponseDto(eq(OTHER_USER_ID), anyString());
        }

        @Test
        @DisplayName("Should throw exception when user tries to unfollow themselves")
        void unfollowUser_WithSameUserId_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            doThrow(new MessageException(SELF_UNFOLLOW_ERROR))
                    .when(followerValidationService).validateNotSelfUnfollow(CURRENT_USER_ID, CURRENT_USER_ID);

            // When & Then
            assertThatThrownBy(() -> followerService.unfollowUser(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(SELF_UNFOLLOW_ERROR);

            verify(followerValidationService, never()).validateAndGetCurrentUser(any());
            verify(followerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when current user is not found")
        void unfollowUser_WithNonExistentCurrentUser_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(CURRENT_USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.unfollowUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(CURRENT_USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerValidationService, never()).validateAndGetUserToUnfollow(any());
            verify(followerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when user to unfollow is not found")
        void unfollowUser_WithNonExistentUserToUnfollow_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToUnfollow(OTHER_USER_ID))
                    .thenThrow(new ResourceNotFoundException(USER_TO_UNFOLLOW_NOT_FOUND + OTHER_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.unfollowUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(USER_TO_UNFOLLOW_NOT_FOUND + OTHER_USER_ID);

            verify(followerValidationService, never()).validateAndGetFollowRelation(any(), any());
            verify(followerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when follow relationship not found")
        void unfollowUser_WithNoFollowerRelationship_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToUnfollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerValidationService.validateAndGetFollowRelation(userToFollow, currentUser))
                    .thenThrow(new ResourceNotFoundException(FOLLOW_RELATION_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> followerService.unfollowUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(FOLLOW_RELATION_NOT_FOUND);

            verify(followerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should include username in unfollow message")
        void unfollowUser_ShouldIncludeUsernameInMessage() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidationService.validateAndGetUserToUnfollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerValidationService.validateAndGetFollowRelation(userToFollow, currentUser))
                    .thenReturn(followerRelationship);
            when(followerMapper.toUnfollowResponseDto(eq(OTHER_USER_ID), contains("other_user")))
                    .thenReturn(unfollowResponseDto);

            // When
            followerService.unfollowUser(OTHER_USER_ID);

            // Then
            verify(followerMapper).toUnfollowResponseDto(eq(OTHER_USER_ID), contains("other_user"));
        }
    }

    @Nested
    @DisplayName("getFollowers() tests")
    class GetFollowersTests {

        @Test
        @DisplayName("Should return followers list when user has followers")
        void getFollowers_WithFollowers_ShouldReturnFollowersList() {
            // Given
            List<Follower> followers = List.of(followerRelationship);
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.findVisibleFollowersByUser(eq(currentUser), any(LocalDateTime.class))).thenReturn(followers);
            when(followerMapper.toFollowersListResponseDto(CURRENT_USER_ID, followers))
                    .thenReturn(followersListResponseDto);

            // When
            FollowersListResponseDTO result = followerService.getFollowers(CURRENT_USER_ID);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(followersListResponseDto);

            verify(followerRepository).findVisibleFollowersByUser(eq(currentUser), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return empty list when user has no followers")
        void getFollowers_WithNoFollowers_ShouldReturnEmptyList() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.findVisibleFollowersByUser(eq(currentUser), any(LocalDateTime.class))).thenReturn(List.of());
            when(followerMapper.toFollowersListResponseDto(CURRENT_USER_ID, List.of()))
                    .thenReturn(followersListResponseDto);

            // When
            FollowersListResponseDTO result = followerService.getFollowers(CURRENT_USER_ID);

            // Then
            assertThat(result).isNotNull();
            verify(followerRepository).findVisibleFollowersByUser(eq(currentUser), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getFollowers_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.getFollowers(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerRepository, never()).findVisibleFollowersByUser(any(), any());
        }

        @Test
        @DisplayName("Should use mapper to create response DTO")
        void getFollowers_ShouldUseMapper() {
            // Given
            List<Follower> followers = List.of(followerRelationship);
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.findVisibleFollowersByUser(eq(currentUser), any(LocalDateTime.class))).thenReturn(followers);
            when(followerMapper.toFollowersListResponseDto(CURRENT_USER_ID, followers))
                    .thenReturn(followersListResponseDto);

            // When
            followerService.getFollowers(CURRENT_USER_ID);

            // Then
            verify(followerMapper).toFollowersListResponseDto(CURRENT_USER_ID, followers);
        }
    }

    @Nested
    @DisplayName("getFollowing() tests")
    class GetFollowingTests {

        @Test
        @DisplayName("Should return following list when user follows others")
        void getFollowing_WithFollowing_ShouldReturnFollowingList() {
            // Given
            List<Follower> following = List.of(followerRelationship);
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.findVisibleFollowingsByFollower(eq(currentUser), any(LocalDateTime.class))).thenReturn(following);
            when(followerMapper.toFollowingListResponseDto(CURRENT_USER_ID, following))
                    .thenReturn(followersListResponseDto);

            // When
            FollowersListResponseDTO result = followerService.getFollowing(CURRENT_USER_ID);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(followersListResponseDto);

            verify(followerRepository).findVisibleFollowingsByFollower(eq(currentUser), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return empty list when user follows no one")
        void getFollowing_WithNoFollowing_ShouldReturnEmptyList() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.findVisibleFollowingsByFollower(eq(currentUser), any(LocalDateTime.class))).thenReturn(List.of());
            when(followerMapper.toFollowingListResponseDto(CURRENT_USER_ID, List.of()))
                    .thenReturn(followersListResponseDto);

            // When
            FollowersListResponseDTO result = followerService.getFollowing(CURRENT_USER_ID);

            // Then
            assertThat(result).isNotNull();
            verify(followerRepository).findVisibleFollowingsByFollower(eq(currentUser), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getFollowing_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.getFollowing(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerRepository, never()).findVisibleFollowingsByFollower(any(), any());
        }

        @Test
        @DisplayName("Should use mapper to create response DTO")
        void getFollowing_ShouldUseMapper() {
            // Given
            List<Follower> following = List.of(followerRelationship);
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.findVisibleFollowingsByFollower(eq(currentUser), any(LocalDateTime.class))).thenReturn(following);
            when(followerMapper.toFollowingListResponseDto(CURRENT_USER_ID, following))
                    .thenReturn(followersListResponseDto);

            // When
            followerService.getFollowing(CURRENT_USER_ID);

            // Then
            verify(followerMapper).toFollowingListResponseDto(CURRENT_USER_ID, following);
        }
    }

    @Nested
    @DisplayName("getFollowerCount() tests")
    class GetFollowerCountTests {

        @Test
        @DisplayName("Should return follower count when user has followers")
        void getFollowerCount_WithFollowers_ShouldReturnCount() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.countVisibleByUser(eq(currentUser), any(LocalDateTime.class))).thenReturn(10L);

            // When
            long result = followerService.getFollowerCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isEqualTo(10L);
            verify(followerRepository).countVisibleByUser(eq(currentUser), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return zero when user has no followers")
        void getFollowerCount_WithNoFollowers_ShouldReturnZero() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.countVisibleByUser(eq(currentUser), any(LocalDateTime.class))).thenReturn(0L);

            // When
            long result = followerService.getFollowerCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getFollowerCount_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.getFollowerCount(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerRepository, never()).countVisibleByUser(any(), any());
        }

        @Test
        @DisplayName("Should return the repository count, which excludes deleted followers like the followers list")
        void getFollowerCount_WithDeletedFollower_ShouldReturnOnlyActiveFollowersCount() {
            // Given: two follow relations, one from a deleted user; countVisibleByUser filters deletedAt IS NULL
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.countVisibleByUser(eq(currentUser), any(LocalDateTime.class))).thenReturn(1L);

            // When
            long result = followerService.getFollowerCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isEqualTo(1L);
            verify(followerRepository).countVisibleByUser(eq(currentUser), any(LocalDateTime.class));
            verifyNoMoreInteractions(followerRepository);
        }
    }

    @Nested
    @DisplayName("getFollowingCount() tests")
    class GetFollowingCountTests {

        @Test
        @DisplayName("Should return following count when user follows others")
        void getFollowingCount_WithFollowing_ShouldReturnCount() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.countVisibleByFollower(eq(currentUser), any(LocalDateTime.class))).thenReturn(15L);

            // When
            long result = followerService.getFollowingCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isEqualTo(15L);
            verify(followerRepository).countVisibleByFollower(eq(currentUser), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return zero when user follows no one")
        void getFollowingCount_WithNoFollowing_ShouldReturnZero() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.countVisibleByFollower(eq(currentUser), any(LocalDateTime.class))).thenReturn(0L);

            // When
            long result = followerService.getFollowingCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getFollowingCount_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.getFollowingCount(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerRepository, never()).countVisibleByFollower(any(), any());
        }

        @Test
        @DisplayName("Should return the repository count, which excludes deleted followed users like the following list")
        void getFollowingCount_WithDeletedFollowedUser_ShouldReturnOnlyActiveFollowingCount() {
            // Given: follows two users, one of them deleted; countVisibleByFollower filters deletedAt IS NULL
            when(followerValidationService.validateAndGetVisibleUser(CURRENT_USER_ID, false)).thenReturn(currentUser);
            when(followerRepository.countVisibleByFollower(eq(currentUser), any(LocalDateTime.class))).thenReturn(1L);

            // When
            long result = followerService.getFollowingCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isEqualTo(1L);
            verify(followerRepository).countVisibleByFollower(eq(currentUser), any(LocalDateTime.class));
            verifyNoMoreInteractions(followerRepository);
        }
    }

    @Nested
    @DisplayName("isFollowing() tests")
    class IsFollowingTests {

        @Test
        @DisplayName("Should return true when user is following another user")
        void isFollowing_WhenFollowing_ShouldReturnTrue() {
            // Given
            when(followerValidationService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID, false)).thenReturn(true);

            // When
            boolean result = followerService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID);

            // Then
            assertThat(result).isTrue();
            verify(followerValidationService).isFollowing(CURRENT_USER_ID, OTHER_USER_ID, false);
        }

        @Test
        @DisplayName("Should return false when user is not following another user")
        void isFollowing_WhenNotFollowing_ShouldReturnFalse() {
            // Given
            when(followerValidationService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID, false)).thenReturn(false);

            // When
            boolean result = followerService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID);

            // Then
            assertThat(result).isFalse();
            verify(followerValidationService).isFollowing(CURRENT_USER_ID, OTHER_USER_ID, false);
        }

        @Test
        @DisplayName("Should delegate to validation service")
        void isFollowing_ShouldDelegateToValidationService() {
            // Given
            when(followerValidationService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID, false)).thenReturn(true);

            // When
            followerService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID);

            // Then
            verify(followerValidationService).isFollowing(CURRENT_USER_ID, OTHER_USER_ID, false);
        }
    }

    @Nested
    @DisplayName("Blocked users are hidden from non-admins (spec 45)")
    class BlockedUsersVisibilityTests {

        private static final Long ADMIN_ID = 9L;

        private void givenCaller(Long callerId, boolean isAdmin) {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(callerId);
            when(followerValidationService.isAdmin(callerId)).thenReturn(isAdmin);
            when(followerValidationService.validateAndGetVisibleUser(OTHER_USER_ID, isAdmin)).thenReturn(userToFollow);
        }

        @Test
        @DisplayName("getFollowers of a non-admin uses findVisibleFollowersByUser")
        void getFollowers_WhenNotAdmin_ShouldUseVisibleQuery() {
            givenCaller(CURRENT_USER_ID, false);
            when(followerRepository.findVisibleFollowersByUser(eq(userToFollow), any(LocalDateTime.class))).thenReturn(List.of());

            followerService.getFollowers(OTHER_USER_ID);

            verify(followerRepository).findVisibleFollowersByUser(eq(userToFollow), any(LocalDateTime.class));
            verify(followerRepository, never()).findFollowersByUser(any());
        }

        @Test
        @DisplayName("getFollowers of an ADMIN uses findFollowersByUser")
        void getFollowers_WhenAdmin_ShouldUseUnfilteredQuery() {
            givenCaller(ADMIN_ID, true);
            when(followerRepository.findFollowersByUser(userToFollow)).thenReturn(List.of());

            followerService.getFollowers(OTHER_USER_ID);

            verify(followerRepository).findFollowersByUser(userToFollow);
            verify(followerRepository, never()).findVisibleFollowersByUser(any(), any());
        }

        @Test
        @DisplayName("getFollowing of a non-admin uses findVisibleFollowingsByFollower")
        void getFollowing_WhenNotAdmin_ShouldUseVisibleQuery() {
            givenCaller(CURRENT_USER_ID, false);
            when(followerRepository.findVisibleFollowingsByFollower(eq(userToFollow), any(LocalDateTime.class))).thenReturn(List.of());

            followerService.getFollowing(OTHER_USER_ID);

            verify(followerRepository).findVisibleFollowingsByFollower(eq(userToFollow), any(LocalDateTime.class));
            verify(followerRepository, never()).findFollowingsByFollower(any());
        }

        @Test
        @DisplayName("getFollowing of an ADMIN uses findFollowingsByFollower")
        void getFollowing_WhenAdmin_ShouldUseUnfilteredQuery() {
            givenCaller(ADMIN_ID, true);
            when(followerRepository.findFollowingsByFollower(userToFollow)).thenReturn(List.of());

            followerService.getFollowing(OTHER_USER_ID);

            verify(followerRepository).findFollowingsByFollower(userToFollow);
            verify(followerRepository, never()).findVisibleFollowingsByFollower(any(), any());
        }

        @Test
        @DisplayName("getFollowerCount of a non-admin uses countVisibleByUser with now")
        void getFollowerCount_WhenNotAdmin_ShouldUseVisibleQuery() {
            givenCaller(CURRENT_USER_ID, false);
            when(followerRepository.countVisibleByUser(eq(userToFollow), any(LocalDateTime.class))).thenReturn(1L);
            LocalDateTime before = LocalDateTime.now();

            assertThat(followerService.getFollowerCount(OTHER_USER_ID)).isEqualTo(1L);

            ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(followerRepository).countVisibleByUser(eq(userToFollow), nowCaptor.capture());
            assertThat(nowCaptor.getValue()).isBetween(before, LocalDateTime.now());
            verify(followerRepository, never()).countByUser(any());
        }

        @Test
        @DisplayName("getFollowerCount of an ADMIN uses countByUser")
        void getFollowerCount_WhenAdmin_ShouldUseUnfilteredQuery() {
            givenCaller(ADMIN_ID, true);
            when(followerRepository.countByUser(userToFollow)).thenReturn(2L);

            assertThat(followerService.getFollowerCount(OTHER_USER_ID)).isEqualTo(2L);

            verify(followerRepository, never()).countVisibleByUser(any(), any());
        }

        @Test
        @DisplayName("getFollowingCount of a non-admin uses countVisibleByFollower")
        void getFollowingCount_WhenNotAdmin_ShouldUseVisibleQuery() {
            givenCaller(CURRENT_USER_ID, false);
            when(followerRepository.countVisibleByFollower(eq(userToFollow), any(LocalDateTime.class))).thenReturn(0L);

            assertThat(followerService.getFollowingCount(OTHER_USER_ID)).isZero();

            verify(followerRepository, never()).countByFollower(any());
        }

        @Test
        @DisplayName("getFollowingCount of an ADMIN uses countByFollower")
        void getFollowingCount_WhenAdmin_ShouldUseUnfilteredQuery() {
            givenCaller(ADMIN_ID, true);
            when(followerRepository.countByFollower(userToFollow)).thenReturn(1L);

            assertThat(followerService.getFollowingCount(OTHER_USER_ID)).isEqualTo(1L);

            verify(followerRepository, never()).countVisibleByFollower(any(), any());
        }

        @Test
        @DisplayName("A blocked user's lists are not found for a non-admin")
        void getFollowers_WhenUserBlockedAndNotAdmin_ShouldThrowNotFound() {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidationService.isAdmin(CURRENT_USER_ID)).thenReturn(false);
            when(followerValidationService.validateAndGetVisibleUser(OTHER_USER_ID, false))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + OTHER_USER_ID));

            assertThatThrownBy(() -> followerService.getFollowers(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(USER_NOT_FOUND + OTHER_USER_ID);

            verifyNoInteractions(followerRepository);
        }

        @Test
        @DisplayName("isFollowing passes whether the caller is an ADMIN to the validation")
        void isFollowing_WhenAdmin_ShouldPassCallerIsAdmin() {
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(ADMIN_ID);
            when(followerValidationService.isAdmin(ADMIN_ID)).thenReturn(true);
            when(followerValidationService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID, true)).thenReturn(true);

            assertThat(followerService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID)).isTrue();
        }
    }
}
