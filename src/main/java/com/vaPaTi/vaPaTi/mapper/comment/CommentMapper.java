package com.vaPaTi.vaPaTi.mapper.comment;

import com.vaPaTi.vaPaTi.dtos.comment.CommentResponseDTO;
import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.entity.user.UserInfo;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    // createdAt is set by @PrePersist
    public Comment toEntity(String text, Publication publication, User author) {
        Comment comment = new Comment();
        comment.setText(text);
        comment.setPublication(publication);
        comment.setUser(author);
        return comment;
    }

    public CommentResponseDTO toDTO(Comment comment) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());
        // The read-only publicationId is only loaded from the database: right after a save it is still null
        dto.setPublicationId(comment.getPublicationId() != null
                ? comment.getPublicationId()
                : comment.getPublication().getId());

        UserInfo userInfo = comment.getUser().getUserInfo();
        dto.setUserId(comment.getUser().getId());
        dto.setFirstName(userInfo.getFirstName());
        dto.setLastName(userInfo.getLastName());
        dto.setUserName(userInfo.getUserName());
        return dto;
    }
}
