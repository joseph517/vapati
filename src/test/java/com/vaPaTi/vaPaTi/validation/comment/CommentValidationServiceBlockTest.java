package com.vaPaTi.vaPaTi.validation.comment;

import com.vaPaTi.vaPaTi.entity.publication.Publication;
import com.vaPaTi.vaPaTi.entity.user.User;
import com.vaPaTi.vaPaTi.exception.ForbiddenActionException;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import com.vaPaTi.vaPaTi.validation.userblock.UserBlockValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentValidationService - Block Tests")
class CommentValidationServiceBlockTest {

    private static final Long CALLER_ID = 2L;
    private static final Long PUBLICATION_AUTHOR_ID = 1L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PublicationValidationService publicationValidationService;

    @Mock
    private AccountStatusValidationService accountStatusValidationService;

    @Mock
    private TextValidationService textValidationService;

    @Mock
    private RoleValidationService roleValidationService;

    @Mock
    private UserBlockValidationService userBlockValidationService;

    @InjectMocks
    private CommentValidationService commentValidationService;

    private Publication publication;

    @BeforeEach
    void setUp() {
        User author = User.builder().id(PUBLICATION_AUTHOR_ID).build();
        publication = Publication.builder().id(10L).user(author).build();
    }

    @Test
    @DisplayName("Should delegate to the shared check with the caller and the publication author")
    void shouldDelegateWithCallerAndPublicationAuthor() {
        assertThatCode(() -> commentValidationService.validateNoBlockWithPublicationAuthor(publication, CALLER_ID))
                .doesNotThrowAnyException();

        verify(userBlockValidationService).validateNoBlockBetween(CALLER_ID, PUBLICATION_AUTHOR_ID);
        verifyNoInteractions(commentRepository, publicationValidationService, accountStatusValidationService,
                textValidationService, roleValidationService);
    }

    @Test
    @DisplayName("Should propagate the 403 of the shared check with its message")
    void shouldPropagateForbidden_whenBlocked() {
        doThrow(new ForbiddenActionException("You cannot interact with this user"))
                .when(userBlockValidationService).validateNoBlockBetween(CALLER_ID, PUBLICATION_AUTHOR_ID);

        assertThatThrownBy(() -> commentValidationService.validateNoBlockWithPublicationAuthor(publication, CALLER_ID))
                .isExactlyInstanceOf(ForbiddenActionException.class)
                .hasMessage("You cannot interact with this user");
    }
}
