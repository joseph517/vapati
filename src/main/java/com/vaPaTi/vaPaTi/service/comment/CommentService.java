package com.vaPaTi.vaPaTi.service.comment;

import com.vaPaTi.vaPaTi.dtos.comment.CommentResponseDTO;
import com.vaPaTi.vaPaTi.dtos.comment.CreateCommentDTO;
import com.vaPaTi.vaPaTi.entity.comment.Comment;
import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.mapper.comment.CommentMapper;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.repository.user.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import com.vaPaTi.vaPaTi.validation.comment.CommentValidationService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Not @Transactional: every method makes at most a single write
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentValidationService commentValidationService;
    private final PublicationValidationService publicationValidationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    public CommentResponseDTO createComment(Long publicationId, CreateCommentDTO dto) {
        Long callerId = authenticatedUserService.getAuthenticatedUserId();
        // Visibility goes before the text: a hidden publication answers 404 even with a blank text
        Publication publication = publicationValidationService.validateAndGetVisiblePublication(publicationId, callerId);
        String text = commentValidationService.validateAndNormalizeText(dto.getText());

        // getReferenceById returns a proxy: no SELECT of the caller just to link it, the token already proved it exists
        Comment comment = commentRepository.save(
                commentMapper.toEntity(text, publication, userRepository.getReferenceById(callerId)));

        return commentMapper.toDTO(comment);
    }

    public Page<CommentResponseDTO> getCommentsByPublicationId(Long publicationId, Pageable pageable) {
        Long callerId = authenticatedUserService.findAuthenticatedUserId().orElse(null);
        publicationValidationService.validateAndGetVisiblePublication(publicationId, callerId);

        // An ADMIN also sees the comments of banned or suspended authors
        Page<Comment> page = commentValidationService.canSeeSanctionedAuthors(callerId)
                ? commentRepository.findAllByPublicationId(publicationId, pageable)
                : commentRepository.findVisibleByPublicationId(publicationId, LocalDateTime.now(), pageable);

        return page.map(commentMapper::toDTO);
    }

    public void deleteComment(Long commentId) {
        Long callerId = authenticatedUserService.getAuthenticatedUserId();
        Comment comment = commentValidationService.validateAndGetDeletableComment(commentId, callerId);

        // Soft delete through @SQLDelete
        commentRepository.delete(comment);
    }
}
