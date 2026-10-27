package com.vaPaTi.vaPaTi.mapper.comment;

import com.vaPaTi.vaPaTi.dtos.comment.CommentResponseDTO;
import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommentMapper Tests")
class CommentMapperTest {

    private final CommentMapper commentMapper = new CommentMapper();

    private User author;
    private Publication publication;

    @BeforeEach
    void setUp() {
        UserInfo userInfo = new UserInfo();
        userInfo.setFirstName("Ana");
        userInfo.setLastName("Commenter");
        userInfo.setUserName("ana_commenter");

        author = new User();
        author.setId(5L);
        author.setUserInfo(userInfo);

        publication = new Publication();
        publication.setId(10L);
    }

    @Test
    @DisplayName("toEntity should set text, publication and user and leave createdAt null")
    void toEntity_ShouldSetFieldsAndLeaveCreatedAtNull() {
        Comment comment = commentMapper.toEntity("Great news", publication, author);

        assertThat(comment.getText()).isEqualTo("Great news");
        assertThat(comment.getPublication()).isSameAs(publication);
        assertThat(comment.getUser()).isSameAs(author);
        assertThat(comment.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("toDTO should copy the comment and author data")
    void toDTO_ShouldCopyAllFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        Comment comment = Comment.builder()
                .id(1L).text("Great news").createdAt(createdAt)
                .user(author).publication(publication).publicationId(10L)
                .build();

        CommentResponseDTO dto = commentMapper.toDTO(comment);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getText()).isEqualTo("Great news");
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        assertThat(dto.getPublicationId()).isEqualTo(10L);
        assertThat(dto.getUserId()).isEqualTo(5L);
        assertThat(dto.getFirstName()).isEqualTo("Ana");
        assertThat(dto.getLastName()).isEqualTo("Commenter");
        assertThat(dto.getUserName()).isEqualTo("ana_commenter");
    }

    @Test
    @DisplayName("toDTO should take the id from the publication when publicationId is null")
    void toDTO_WithNullPublicationId_ShouldUsePublicationId() {
        Comment comment = Comment.builder()
                .id(1L).text("Great news").user(author).publication(publication)
                .build();

        CommentResponseDTO dto = commentMapper.toDTO(comment);

        assertThat(dto.getPublicationId()).isEqualTo(10L);
    }
}
