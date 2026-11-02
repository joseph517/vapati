package com.vaPaTi.vaPaTi.validation.comment;

import com.vaPaTi.vaPaTi.exception.MessageException;
import com.vaPaTi.vaPaTi.repository.comment.CommentRepository;
import com.vaPaTi.vaPaTi.validation.auth.AccountStatusValidationService;
import com.vaPaTi.vaPaTi.validation.common.RoleValidationService;
import com.vaPaTi.vaPaTi.validation.common.TextValidationService;
import com.vaPaTi.vaPaTi.validation.publication.PublicationValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentValidationService - Text Tests")
class CommentValidationServiceTextTest {

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

    @InjectMocks
    private CommentValidationService commentValidationService;

    @Test
    @DisplayName("Should delegate to normalizeRequiredText with \"Text\" and 280")
    void shouldDelegateWithLabelAndMaxLength() {
        when(textValidationService.normalizeRequiredText("  Great work  ", "Text", 280)).thenReturn("Great work");

        commentValidationService.validateAndNormalizeText("  Great work  ");

        verify(textValidationService).normalizeRequiredText("  Great work  ", "Text", 280);
    }

    @Test
    @DisplayName("Should return the normalized text")
    void shouldReturnNormalizedText() {
        when(textValidationService.normalizeRequiredText("  Great work  ", "Text", 280)).thenReturn("Great work");

        String result = commentValidationService.validateAndNormalizeText("  Great work  ");

        assertThat(result).isEqualTo("Great work");
    }

    @Test
    @DisplayName("Should propagate the MessageException of the text service")
    void shouldPropagateMessageException() {
        when(textValidationService.normalizeRequiredText("   ", "Text", 280))
                .thenThrow(new MessageException("Text cannot be blank"));

        assertThatThrownBy(() -> commentValidationService.validateAndNormalizeText("   "))
                .isInstanceOf(MessageException.class)
                .hasMessage("Text cannot be blank");
    }
}
