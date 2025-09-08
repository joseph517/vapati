package com.vaPaTi.vaPaTi.service.Authentication;

import com.vaPaTi.vaPaTi.entity.RevokedToken;
import com.vaPaTi.vaPaTi.repository.RevokedTokenRepository;
import com.vaPaTi.vaPaTi.service.TokenBlackListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlackListService Tests")
class TokenBlackListServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private TokenBlackListService tokenBlackListService;

    private String validToken;
    private LocalDateTime futureExpirationDate;
    private LocalDateTime currentDateTime;

    @BeforeEach
    void setUp() {
        validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        futureExpirationDate = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        currentDateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
    }

    @Nested
    @DisplayName("isTokenRevoked() Tests")
    class IsTokenRevokedTests {

        @Test
        @DisplayName("Debe retornar true cuando el token está revocado")
        void shouldReturnTrueWhenTokenIsRevoked() {
            // Given
            when(revokedTokenRepository.existsByToken(validToken)).thenReturn(true);

            // When
            boolean result = tokenBlackListService.isTokenRevoked(validToken);

            // Then
            assertThat(result).isTrue();
            verify(revokedTokenRepository).existsByToken(validToken);
        }

        @Test
        @DisplayName("Debe retornar false cuando el token no está revocado")
        void shouldReturnFalseWhenTokenIsNotRevoked() {
            // Given
            when(revokedTokenRepository.existsByToken(validToken)).thenReturn(false);

            // When
            boolean result = tokenBlackListService.isTokenRevoked(validToken);

            // Then
            assertThat(result).isFalse();
            verify(revokedTokenRepository).existsByToken(validToken);
        }

        @Test
        @DisplayName("Debe manejar token nulo correctamente")
        void shouldHandleNullTokenCorrectly() {
            // Given
            String nullToken = null;
            when(revokedTokenRepository.existsByToken(nullToken)).thenReturn(false);

            // When
            boolean result = tokenBlackListService.isTokenRevoked(nullToken);

            // Then
            assertThat(result).isFalse();
            verify(revokedTokenRepository).existsByToken(nullToken);
        }

        @Test
        @DisplayName("Debe manejar token vacío correctamente")
        void shouldHandleEmptyTokenCorrectly() {
            // Given
            String emptyToken = "";
            when(revokedTokenRepository.existsByToken(emptyToken)).thenReturn(false);

            // When
            boolean result = tokenBlackListService.isTokenRevoked(emptyToken);

            // Then
            assertThat(result).isFalse();
            verify(revokedTokenRepository).existsByToken(emptyToken);
        }
    }

    @Nested
    @DisplayName("revokeToken() Tests")
    class RevokeTokenTests {

        @Test
        @DisplayName("Debe revocar token cuando no está previamente revocado")
        void shouldRevokeTokenWhenNotPreviouslyRevoked() {
            // Given
            when(revokedTokenRepository.existsByToken(validToken)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(validToken, futureExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByToken(validToken);
            verify(revokedTokenRepository).save(argThat(revokedToken ->
                    revokedToken.getToken().equals(validToken) &&
                            revokedToken.getExpirationDate().equals(futureExpirationDate)
            ));
        }

        @Test
        @DisplayName("No debe revocar token cuando ya está revocado")
        void shouldNotRevokeTokenWhenAlreadyRevoked() {
            // Given
            when(revokedTokenRepository.existsByToken(validToken)).thenReturn(true);

            // When
            tokenBlackListService.revokeToken(validToken, futureExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByToken(validToken);
            verify(revokedTokenRepository, never()).save(any(RevokedToken.class));
        }

        @Test
        @DisplayName("Debe verificar el orden de ejecución: primero verificar existencia, luego guardar")
        void shouldVerifyExecutionOrder() {
            // Given
            when(revokedTokenRepository.existsByToken(validToken)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));
            var inOrder = inOrder(revokedTokenRepository);

            // When
            tokenBlackListService.revokeToken(validToken, futureExpirationDate);

            // Then
            inOrder.verify(revokedTokenRepository).existsByToken(validToken);
            inOrder.verify(revokedTokenRepository).save(any(RevokedToken.class));
        }

        @Test
        @DisplayName("Debe manejar token nulo al intentar revocar")
        void shouldHandleNullTokenWhenRevoking() {
            // Given
            String nullToken = null;
            when(revokedTokenRepository.existsByToken(nullToken)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(nullToken, futureExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByToken(nullToken);
            verify(revokedTokenRepository).save(argThat(revokedToken ->
                    revokedToken.getToken() == null &&
                            revokedToken.getExpirationDate().equals(futureExpirationDate)
            ));
        }

        @Test
        @DisplayName("Debe manejar fecha de expiración nula al revocar")
        void shouldHandleNullExpirationDateWhenRevoking() {
            // Given
            LocalDateTime nullExpirationDate = null;
            when(revokedTokenRepository.existsByToken(validToken)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(validToken, nullExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByToken(validToken);
            verify(revokedTokenRepository).save(argThat(revokedToken ->
                    revokedToken.getToken().equals(validToken) &&
                            revokedToken.getExpirationDate() == null
            ));
        }

        @Test
        @DisplayName("Debe manejar fecha de expiración en el pasado")
        void shouldHandlePastExpirationDate() {
            // Given
            LocalDateTime pastExpirationDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            when(revokedTokenRepository.existsByToken(validToken)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(validToken, pastExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByToken(validToken);
            verify(revokedTokenRepository).save(argThat(revokedToken ->
                    revokedToken.getToken().equals(validToken) &&
                            revokedToken.getExpirationDate().equals(pastExpirationDate)
            ));
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens() Tests")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("Debe limpiar tokens expirados usando la fecha actual")
        void shouldCleanupExpiredTokensUsingCurrentDate() {
            // Given - Mockeamos LocalDateTime.now()
            try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
                mockedLocalDateTime.when(LocalDateTime::now).thenReturn(currentDateTime);
                doNothing().when(revokedTokenRepository).deleteExpiredTokens(currentDateTime);

                // When
                tokenBlackListService.cleanupExpiredTokens();

                // Then
                verify(revokedTokenRepository).deleteExpiredTokens(currentDateTime);
                mockedLocalDateTime.verify(LocalDateTime::now);
            }
        }

        @Test
        @DisplayName("Debe llamar al método de limpieza exactamente una vez")
        void shouldCallCleanupMethodExactlyOnce() {
            // Given
            try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
                mockedLocalDateTime.when(LocalDateTime::now).thenReturn(currentDateTime);
                doNothing().when(revokedTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));

                // When
                tokenBlackListService.cleanupExpiredTokens();

                // Then
                verify(revokedTokenRepository, times(1)).deleteExpiredTokens(any(LocalDateTime.class));
            }
        }

        @Test
        @DisplayName("Debe pasar la fecha correcta al repositorio para limpieza")
        void shouldPassCorrectDateToRepositoryForCleanup() {
            // Given
            LocalDateTime specificDateTime = LocalDateTime.of(2024, 7, 20, 14, 30, 45);
            try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
                mockedLocalDateTime.when(LocalDateTime::now).thenReturn(specificDateTime);
                doNothing().when(revokedTokenRepository).deleteExpiredTokens(specificDateTime);

                // When
                tokenBlackListService.cleanupExpiredTokens();

                // Then
                verify(revokedTokenRepository).deleteExpiredTokens(eq(specificDateTime));
            }
        }

        @Test
        @DisplayName("Debe manejar excepción del repositorio durante limpieza")
        void shouldHandleRepositoryExceptionDuringCleanup() {
            // Given
            try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
                mockedLocalDateTime.when(LocalDateTime::now).thenReturn(currentDateTime);
                doThrow(new RuntimeException("Database error")).when(revokedTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));

                // When & Then
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                    tokenBlackListService.cleanupExpiredTokens();
                });

                verify(revokedTokenRepository).deleteExpiredTokens(currentDateTime);
            }
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenariosTests {

        @Test
        @DisplayName("Escenario completo: verificar -> revocar -> verificar nuevamente")
        void shouldHandleCompleteScenarioCheckRevokeCheckAgain() {
            // Given
            when(revokedTokenRepository.existsByToken(validToken))
                    .thenReturn(false)
                    .thenReturn(false)
                    .thenReturn(true);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            boolean initialCheck = tokenBlackListService.isTokenRevoked(validToken);
            tokenBlackListService.revokeToken(validToken, futureExpirationDate);
            boolean finalCheck = tokenBlackListService.isTokenRevoked(validToken);

            // Then
            assertThat(initialCheck).isFalse();
            assertThat(finalCheck).isTrue();
            verify(revokedTokenRepository, times(3)).existsByToken(validToken); // corregido
            verify(revokedTokenRepository).save(any(RevokedToken.class));
        }


        @Test
        @DisplayName("Debe evitar doble revocación del mismo token")
        void shouldAvoidDoubleRevocationOfSameToken() {
            // Given
            when(revokedTokenRepository.existsByToken(validToken))
                    .thenReturn(false)  // Primera llamada: no existe
                    .thenReturn(true);  // Segunda llamada: ya existe
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(validToken, futureExpirationDate);
            tokenBlackListService.revokeToken(validToken, futureExpirationDate);

            // Then
            verify(revokedTokenRepository, times(2)).existsByToken(validToken);
            verify(revokedTokenRepository, times(1)).save(any(RevokedToken.class));
        }
    }

}
