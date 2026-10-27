package com.vaPaTi.vaPaTi.validation.comment;

import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentValidationService {

    static final int MAX_TEXT_LENGTH = 280;
    private static final String COMMENT_NOT_FOUND = "Comment not found with ID: ";
    private static final String DELETE_FORBIDDEN =
            "Only the comment author, the publication author or an admin can delete this comment";

    private final CommentRepository commentRepository;
    private final PublicationValidationService publicationValidationService;
    private final AccountStatusValidationService accountStatusValidationService;
    private final TextValidationService textValidationService;
    private final RoleValidationService roleValidationService;

    public String validateAndNormalizeText(String text) {
        return textValidationService.normalizeRequiredText(text, "Text", MAX_TEXT_LENGTH);
    }

    // A comment is visible when its author is active (or sanctioned, for an ADMIN) and its publication is visible. Anything hidden gets the same 404 as a missing comment, never the publication's (it would reveal the comment exists)
    public Comment validateAndGetVisibleComment(Long commentId, Long callerId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(COMMENT_NOT_FOUND + commentId));

        if (comment.getUser() == null) {
            throw new ResourceNotFoundException(COMMENT_NOT_FOUND + commentId);
        }

        if (accountStatusValidationService.isSanctioned(comment.getUser())
                && !roleValidationService.isAdmin(callerId)) {
            throw new ResourceNotFoundException(COMMENT_NOT_FOUND + commentId);
        }

        if (publicationValidationService.findVisiblePublication(comment.getPublicationId(), callerId).isEmpty()) {
            throw new ResourceNotFoundException(COMMENT_NOT_FOUND + commentId);
        }

        return comment;
    }

    public Comment validateAndGetDeletableComment(Long commentId, Long callerId) {
        Comment comment = validateAndGetVisibleComment(commentId, callerId);

        if (callerId.equals(comment.getUser().getId())) {
            return comment;
        }

        if (callerId.equals(comment.getPublication().getUser().getId())) {
            return comment;
        }

        if (roleValidationService.isAdmin(callerId)) {
            return comment;
        }

        throw new ForbiddenActionException(DELETE_FORBIDDEN);
    }

    public boolean canSeeSanctionedAuthors(Long callerId) {
        return roleValidationService.isAdmin(callerId);
    }
}
