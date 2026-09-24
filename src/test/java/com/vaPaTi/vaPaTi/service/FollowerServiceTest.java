package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.FollowResponseDto;
import com.vaPaTi.vaPaTi.dtos.FollowerUserDto;
import com.vaPaTi.vaPaTi.dtos.FollowersListResponseDto;
import com.vaPaTi.vaPaTi.dtos.UnfollowResponseDto;
import com.vaPaTi.vaPaTi.entity.Follower;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.entity.UserInfo;
import com.vaPaTi.vaPaTi.exception.ConflictException;
import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.mapper.FollowerMapper;
import com.vaPaTi.vaPaTi.repository.FollowerRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.FollowerValidation;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowerService Tests")
class FollowerServiceTest {

    @Mock
    private FollowerRepository followerRepository;
    @Mock
    private FollowerMapper followerMapper;
    @Mock
    private FollowerValidation followerValidation;
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
    private static final String SANCTIONED_USER_ERROR = "You cannot follow a banned or suspended user";
    private static final String FOLLOW_RELATION_NOT_FOUND = "Follow relationship not found";

    private User currentUser;
    private User userToFollow;
    private UserInfo currentUserInfo;
    private UserInfo otherUserInfo;
    private Follower followerRelationship;
    private FollowResponseDto followResponseDto;
    private UnfollowResponseDto unfollowResponseDto;
    private FollowersListResponseDto followersListResponseDto;

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

        FollowerUserDto mockFollowerUser = new FollowerUserDto(
                OTHER_USER_ID,
                "John",
                "Doe",
                "other_user",
                null,
                LocalDateTime.now()
        );

        followResponseDto = new FollowResponseDto(
                "Successfully started following other_user",
                true,
                mockFollowerUser
        );

        unfollowResponseDto = new UnfollowResponseDto(
                "Successfully unfollowed other_user",
                true,
                OTHER_USER_ID
        );

        followersListResponseDto = new FollowersListResponseDto(
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
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerRepository.save(any(Follower.class))).thenReturn(followerRelationship);
            when(followerMapper.toFollowResponseDto(any(Follower.class), anyString())).thenReturn(followResponseDto);

            // When
            FollowResponseDto result = followerService.followUser(OTHER_USER_ID);

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
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerRepository.save(any(Follower.class))).thenReturn(followerRelationship);
            when(followerMapper.toFollowResponseDto(any(Follower.class), anyString())).thenReturn(followResponseDto);

            // When
            followerService.followUser(OTHER_USER_ID);

            // Then
            InOrder inOrder = inOrder(authenticatedUserService, followerValidation, followerRepository, followerMapper);
            inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
            inOrder.verify(followerValidation).validateNotSelfFollow(CURRENT_USER_ID, OTHER_USER_ID);
            inOrder.verify(followerValidation).validateAndGetCurrentUser(CURRENT_USER_ID);
            inOrder.verify(followerValidation).validateAndGetUserToFollow(OTHER_USER_ID);
            inOrder.verify(followerValidation).validateNotSanctioned(userToFollow);
            inOrder.verify(followerValidation).validateNotAlreadyFollowing(userToFollow, currentUser);
            inOrder.verify(followerRepository).save(any(Follower.class));
            inOrder.verify(followerMapper).toFollowResponseDto(any(Follower.class), anyString());
        }

        @Test
        @DisplayName("Should throw exception when user tries to follow themselves")
        void followUser_WithSameUserId_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            doThrow(new MessageException(SELF_FOLLOW_ERROR))
                    .when(followerValidation).validateNotSelfFollow(CURRENT_USER_ID, CURRENT_USER_ID);

            // When & Then
            assertThatThrownBy(() -> followerService.followUser(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(SELF_FOLLOW_ERROR);

            verify(followerValidation, never()).validateAndGetCurrentUser(any());
            verify(followerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when current user is not found")
        void followUser_WithNonExistentCurrentUser_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(CURRENT_USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.followUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(CURRENT_USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerValidation, never()).validateAndGetUserToFollow(any());
            verify(followerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user to follow is not found")
        void followUser_WithNonExistentUserToFollow_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToFollow(OTHER_USER_ID))
                    .thenThrow(new ResourceNotFoundException(USER_TO_FOLLOW_NOT_FOUND + OTHER_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.followUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(USER_TO_FOLLOW_NOT_FOUND + OTHER_USER_ID);

            verify(followerValidation, never()).validateNotSanctioned(any());
            verify(followerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user to follow is banned or suspended")
        void followUser_WhenUserToFollowIsSanctioned_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            doThrow(new MessageException(SANCTIONED_USER_ERROR))
                    .when(followerValidation).validateNotSanctioned(userToFollow);

            // When & Then
            assertThatThrownBy(() -> followerService.followUser(OTHER_USER_ID))
                    .isExactlyInstanceOf(MessageException.class)
                    .hasMessage(SANCTIONED_USER_ERROR);

            verify(followerValidation, never()).validateNotAlreadyFollowing(any(), any());
            verify(followerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when already following user")
        void followUser_WhenAlreadyFollowing_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
            doThrow(new ConflictException(ALREADY_FOLLOWING_ERROR))
                    .when(followerValidation).validateNotAlreadyFollowing(userToFollow, currentUser);

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
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
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
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
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
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToFollow(OTHER_USER_ID)).thenReturn(userToFollow);
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
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToUnfollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerValidation.validateAndGetFollowRelation(userToFollow, currentUser))
                    .thenReturn(followerRelationship);
            when(followerMapper.toUnfollowResponseDto(anyLong(), anyString())).thenReturn(unfollowResponseDto);

            // When
            UnfollowResponseDto result = followerService.unfollowUser(OTHER_USER_ID);

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
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToUnfollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerValidation.validateAndGetFollowRelation(userToFollow, currentUser))
                    .thenReturn(followerRelationship);
            when(followerMapper.toUnfollowResponseDto(anyLong(), anyString())).thenReturn(unfollowResponseDto);

            // When
            followerService.unfollowUser(OTHER_USER_ID);

            // Then
            InOrder inOrder = inOrder(authenticatedUserService, followerValidation, followerRepository, followerMapper);
            inOrder.verify(authenticatedUserService).getAuthenticatedUserId();
            inOrder.verify(followerValidation).validateNotSelfUnfollow(CURRENT_USER_ID, OTHER_USER_ID);
            inOrder.verify(followerValidation).validateAndGetCurrentUser(CURRENT_USER_ID);
            inOrder.verify(followerValidation).validateAndGetUserToUnfollow(OTHER_USER_ID);
            inOrder.verify(followerValidation).validateAndGetFollowRelation(userToFollow, currentUser);
            inOrder.verify(followerRepository).delete(followerRelationship);
            inOrder.verify(followerMapper).toUnfollowResponseDto(eq(OTHER_USER_ID), anyString());
            verify(followerValidation, never()).validateNotSanctioned(any());
        }

        @Test
        @DisplayName("Should throw exception when user tries to unfollow themselves")
        void unfollowUser_WithSameUserId_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            doThrow(new MessageException(SELF_UNFOLLOW_ERROR))
                    .when(followerValidation).validateNotSelfUnfollow(CURRENT_USER_ID, CURRENT_USER_ID);

            // When & Then
            assertThatThrownBy(() -> followerService.unfollowUser(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(SELF_UNFOLLOW_ERROR);

            verify(followerValidation, never()).validateAndGetCurrentUser(any());
            verify(followerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when current user is not found")
        void unfollowUser_WithNonExistentCurrentUser_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(CURRENT_USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.unfollowUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(CURRENT_USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerValidation, never()).validateAndGetUserToUnfollow(any());
            verify(followerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when user to unfollow is not found")
        void unfollowUser_WithNonExistentUserToUnfollow_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToUnfollow(OTHER_USER_ID))
                    .thenThrow(new ResourceNotFoundException(USER_TO_UNFOLLOW_NOT_FOUND + OTHER_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.unfollowUser(OTHER_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(USER_TO_UNFOLLOW_NOT_FOUND + OTHER_USER_ID);

            verify(followerValidation, never()).validateAndGetFollowRelation(any(), any());
            verify(followerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when follow relationship not found")
        void unfollowUser_WithNoFollowerRelationship_ShouldThrowException() {
            // Given
            when(authenticatedUserService.getAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToUnfollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerValidation.validateAndGetFollowRelation(userToFollow, currentUser))
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
            when(followerValidation.validateAndGetCurrentUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerValidation.validateAndGetUserToUnfollow(OTHER_USER_ID)).thenReturn(userToFollow);
            when(followerValidation.validateAndGetFollowRelation(userToFollow, currentUser))
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
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.findFollowersByUser(currentUser)).thenReturn(followers);
            when(followerMapper.toFollowersListResponseDto(CURRENT_USER_ID, followers))
                    .thenReturn(followersListResponseDto);

            // When
            FollowersListResponseDto result = followerService.getFollowers(CURRENT_USER_ID);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(followersListResponseDto);

            verify(followerRepository).findFollowersByUser(currentUser);
        }

        @Test
        @DisplayName("Should return empty list when user has no followers")
        void getFollowers_WithNoFollowers_ShouldReturnEmptyList() {
            // Given
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.findFollowersByUser(currentUser)).thenReturn(List.of());
            when(followerMapper.toFollowersListResponseDto(CURRENT_USER_ID, List.of()))
                    .thenReturn(followersListResponseDto);

            // When
            FollowersListResponseDto result = followerService.getFollowers(CURRENT_USER_ID);

            // Then
            assertThat(result).isNotNull();
            verify(followerRepository).findFollowersByUser(currentUser);
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getFollowers_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.getFollowers(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerRepository, never()).findFollowersByUser(any());
        }

        @Test
        @DisplayName("Should use mapper to create response DTO")
        void getFollowers_ShouldUseMapper() {
            // Given
            List<Follower> followers = List.of(followerRelationship);
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.findFollowersByUser(currentUser)).thenReturn(followers);
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
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.findFollowingsByFollower(currentUser)).thenReturn(following);
            when(followerMapper.toFollowingListResponseDto(CURRENT_USER_ID, following))
                    .thenReturn(followersListResponseDto);

            // When
            FollowersListResponseDto result = followerService.getFollowing(CURRENT_USER_ID);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(followersListResponseDto);

            verify(followerRepository).findFollowingsByFollower(currentUser);
        }

        @Test
        @DisplayName("Should return empty list when user follows no one")
        void getFollowing_WithNoFollowing_ShouldReturnEmptyList() {
            // Given
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.findFollowingsByFollower(currentUser)).thenReturn(List.of());
            when(followerMapper.toFollowingListResponseDto(CURRENT_USER_ID, List.of()))
                    .thenReturn(followersListResponseDto);

            // When
            FollowersListResponseDto result = followerService.getFollowing(CURRENT_USER_ID);

            // Then
            assertThat(result).isNotNull();
            verify(followerRepository).findFollowingsByFollower(currentUser);
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getFollowing_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.getFollowing(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerRepository, never()).findFollowingsByFollower(any());
        }

        @Test
        @DisplayName("Should use mapper to create response DTO")
        void getFollowing_ShouldUseMapper() {
            // Given
            List<Follower> following = List.of(followerRelationship);
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.findFollowingsByFollower(currentUser)).thenReturn(following);
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
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.countByUser(currentUser)).thenReturn(10L);

            // When
            long result = followerService.getFollowerCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isEqualTo(10L);
            verify(followerRepository).countByUser(currentUser);
        }

        @Test
        @DisplayName("Should return zero when user has no followers")
        void getFollowerCount_WithNoFollowers_ShouldReturnZero() {
            // Given
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.countByUser(currentUser)).thenReturn(0L);

            // When
            long result = followerService.getFollowerCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getFollowerCount_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.getFollowerCount(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerRepository, never()).countByUser(any());
        }

        @Test
        @DisplayName("Should return the repository count, which excludes deleted followers like the followers list")
        void getFollowerCount_WithDeletedFollower_ShouldReturnOnlyActiveFollowersCount() {
            // Given: two follow relations, one from a deleted user; countByUser filters deletedAt IS NULL
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.countByUser(currentUser)).thenReturn(1L);

            // When
            long result = followerService.getFollowerCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isEqualTo(1L);
            verify(followerRepository).countByUser(currentUser);
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
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.countByFollower(currentUser)).thenReturn(15L);

            // When
            long result = followerService.getFollowingCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isEqualTo(15L);
            verify(followerRepository).countByFollower(currentUser);
        }

        @Test
        @DisplayName("Should return zero when user follows no one")
        void getFollowingCount_WithNoFollowing_ShouldReturnZero() {
            // Given
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.countByFollower(currentUser)).thenReturn(0L);

            // When
            long result = followerService.getFollowingCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getFollowingCount_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND + CURRENT_USER_ID));

            // When & Then
            assertThatThrownBy(() -> followerService.getFollowingCount(CURRENT_USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(USER_NOT_FOUND + CURRENT_USER_ID);

            verify(followerRepository, never()).countByFollower(any());
        }

        @Test
        @DisplayName("Should return the repository count, which excludes deleted followed users like the following list")
        void getFollowingCount_WithDeletedFollowedUser_ShouldReturnOnlyActiveFollowingCount() {
            // Given: follows two users, one of them deleted; countByFollower filters deletedAt IS NULL
            when(followerValidation.validateAndGetUser(CURRENT_USER_ID)).thenReturn(currentUser);
            when(followerRepository.countByFollower(currentUser)).thenReturn(1L);

            // When
            long result = followerService.getFollowingCount(CURRENT_USER_ID);

            // Then
            assertThat(result).isEqualTo(1L);
            verify(followerRepository).countByFollower(currentUser);
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
            when(followerValidation.isFollowing(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(true);

            // When
            boolean result = followerService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID);

            // Then
            assertThat(result).isTrue();
            verify(followerValidation).isFollowing(CURRENT_USER_ID, OTHER_USER_ID);
        }

        @Test
        @DisplayName("Should return false when user is not following another user")
        void isFollowing_WhenNotFollowing_ShouldReturnFalse() {
            // Given
            when(followerValidation.isFollowing(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(false);

            // When
            boolean result = followerService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID);

            // Then
            assertThat(result).isFalse();
            verify(followerValidation).isFollowing(CURRENT_USER_ID, OTHER_USER_ID);
        }

        @Test
        @DisplayName("Should delegate to validation service")
        void isFollowing_ShouldDelegateToValidationService() {
            // Given
            when(followerValidation.isFollowing(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(true);

            // When
            followerService.isFollowing(CURRENT_USER_ID, OTHER_USER_ID);

            // Then
            verify(followerValidation).isFollowing(CURRENT_USER_ID, OTHER_USER_ID);
        }
    }
}
