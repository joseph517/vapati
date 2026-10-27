package com.vaPaTi.vaPaTi.validation.comment;

import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.exception.ResourceNotFoundException;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import com.vaPaTi.vaPaTi.validation.campaign.CampaignAuthorizationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentValidationService {

    static final int MAX_TEXT_LENGTH = 280;
    private static final String COMMENT_NOT_FOUND = "Comment not found with ID: ";

    private final CommentRepository commentRepository;
    private final PublicationValidationService publicationValidationService;
    private final CampaignAuthorizationService campaignAuthorizationService;
    private final AccountStatusValidationService accountStatusValidationService;
    private final TextValidationService textValidationService;

    public String validateAndNormalizeText(String text) {
        return textValidationService.normalizeRequiredText(text, "Text", MAX_TEXT_LENGTH);
    }

    // A comment is visible when its author is active (or blocked, for an ADMIN) and its publication is visible. Anything hidden gets the same 404 as a missing comment, never the publication's (it would reveal the comment exists)
    public Comment validateAndGetVisibleComment(Long commentId, Long callerId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(COMMENT_NOT_FOUND + commentId));

        if (comment.getUser() == null) {
            throw new ResourceNotFoundException(COMMENT_NOT_FOUND + commentId);
        }

        if (accountStatusValidationService.isBlocked(comment.getUser())
                && !campaignAuthorizationService.isAdmin(callerId)) {
            throw new ResourceNotFoundException(COMMENT_NOT_FOUND + commentId);
        }

        if (publicationValidationService.findVisiblePublication(comment.getPublicationId(), callerId).isEmpty()) {
            throw new ResourceNotFoundException(COMMENT_NOT_FOUND + commentId);
        }

        return comment;
    }

    public boolean canSeeBlockedAuthors(Long callerId) {
        return callerId != null && campaignAuthorizationService.isAdmin(callerId);
    }
}
