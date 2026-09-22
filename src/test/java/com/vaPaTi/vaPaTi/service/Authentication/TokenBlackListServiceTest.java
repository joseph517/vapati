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

    private String validJti;
    private LocalDateTime futureExpirationDate;
    private LocalDateTime currentDateTime;

    @BeforeEach
    void setUp() {
        validJti = "3f1c9a7e-2b4d-4c8e-9f61-0a5b7d2e8c14";
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
            when(revokedTokenRepository.existsByJti(validJti)).thenReturn(true);

            // When
            boolean result = tokenBlackListService.isTokenRevoked(validJti);

            // Then
            assertThat(result).isTrue();
            verify(revokedTokenRepository).existsByJti(validJti);
        }

        @Test
        @DisplayName("Debe retornar false cuando el token no está revocado")
        void shouldReturnFalseWhenTokenIsNotRevoked() {
            // Given
            when(revokedTokenRepository.existsByJti(validJti)).thenReturn(false);

            // When
            boolean result = tokenBlackListService.isTokenRevoked(validJti);

            // Then
            assertThat(result).isFalse();
            verify(revokedTokenRepository).existsByJti(validJti);
        }

        @Test
        @DisplayName("Debe manejar token nulo correctamente")
        void shouldHandleNullTokenCorrectly() {
            // Given
            String nullJti = null;
            when(revokedTokenRepository.existsByJti(nullJti)).thenReturn(false);

            // When
            boolean result = tokenBlackListService.isTokenRevoked(nullJti);

            // Then
            assertThat(result).isFalse();
            verify(revokedTokenRepository).existsByJti(nullJti);
        }

        @Test
        @DisplayName("Debe manejar token vacío correctamente")
        void shouldHandleEmptyTokenCorrectly() {
            // Given
            String emptyJti = "";
            when(revokedTokenRepository.existsByJti(emptyJti)).thenReturn(false);

            // When
            boolean result = tokenBlackListService.isTokenRevoked(emptyJti);

            // Then
            assertThat(result).isFalse();
            verify(revokedTokenRepository).existsByJti(emptyJti);
        }
    }

    @Nested
    @DisplayName("revokeToken() Tests")
    class RevokeTokenTests {

        @Test
        @DisplayName("Debe revocar token cuando no está previamente revocado")
        void shouldRevokeTokenWhenNotPreviouslyRevoked() {
            // Given
            when(revokedTokenRepository.existsByJti(validJti)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(validJti, futureExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByJti(validJti);
            verify(revokedTokenRepository).save(argThat(revokedToken ->
                    revokedToken.getJti().equals(validJti) &&
                            revokedToken.getExpirationDate().equals(futureExpirationDate)
            ));
        }

        @Test
        @DisplayName("No debe revocar token cuando ya está revocado")
        void shouldNotRevokeTokenWhenAlreadyRevoked() {
            // Given
            when(revokedTokenRepository.existsByJti(validJti)).thenReturn(true);

            // When
            tokenBlackListService.revokeToken(validJti, futureExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByJti(validJti);
            verify(revokedTokenRepository, never()).save(any(RevokedToken.class));
        }

        @Test
        @DisplayName("Debe verificar el orden de ejecución: primero verificar existencia, luego guardar")
        void shouldVerifyExecutionOrder() {
            // Given
            when(revokedTokenRepository.existsByJti(validJti)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));
            var inOrder = inOrder(revokedTokenRepository);

            // When
            tokenBlackListService.revokeToken(validJti, futureExpirationDate);

            // Then
            inOrder.verify(revokedTokenRepository).existsByJti(validJti);
            inOrder.verify(revokedTokenRepository).save(any(RevokedToken.class));
        }

        @Test
        @DisplayName("Debe manejar token nulo al intentar revocar")
        void shouldHandleNullTokenWhenRevoking() {
            // Given
            String nullJti = null;
            when(revokedTokenRepository.existsByJti(nullJti)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(nullJti, futureExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByJti(nullJti);
            verify(revokedTokenRepository).save(argThat(revokedToken ->
                    revokedToken.getJti() == null &&
                            revokedToken.getExpirationDate().equals(futureExpirationDate)
            ));
        }

        @Test
        @DisplayName("Debe manejar fecha de expiración nula al revocar")
        void shouldHandleNullExpirationDateWhenRevoking() {
            // Given
            LocalDateTime nullExpirationDate = null;
            when(revokedTokenRepository.existsByJti(validJti)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(validJti, nullExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByJti(validJti);
            verify(revokedTokenRepository).save(argThat(revokedToken ->
                    revokedToken.getJti().equals(validJti) &&
                            revokedToken.getExpirationDate() == null
            ));
        }

        @Test
        @DisplayName("Debe manejar fecha de expiración en el pasado")
        void shouldHandlePastExpirationDate() {
            // Given
            LocalDateTime pastExpirationDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            when(revokedTokenRepository.existsByJti(validJti)).thenReturn(false);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(validJti, pastExpirationDate);

            // Then
            verify(revokedTokenRepository).existsByJti(validJti);
            verify(revokedTokenRepository).save(argThat(revokedToken ->
                    revokedToken.getJti().equals(validJti) &&
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
            when(revokedTokenRepository.existsByJti(validJti))
                    .thenReturn(false)
                    .thenReturn(false)
                    .thenReturn(true);
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            boolean initialCheck = tokenBlackListService.isTokenRevoked(validJti);
            tokenBlackListService.revokeToken(validJti, futureExpirationDate);
            boolean finalCheck = tokenBlackListService.isTokenRevoked(validJti);

            // Then
            assertThat(initialCheck).isFalse();
            assertThat(finalCheck).isTrue();
            verify(revokedTokenRepository, times(3)).existsByJti(validJti); // corregido
            verify(revokedTokenRepository).save(any(RevokedToken.class));
        }


        @Test
        @DisplayName("Debe evitar doble revocación del mismo token")
        void shouldAvoidDoubleRevocationOfSameToken() {
            // Given
            when(revokedTokenRepository.existsByJti(validJti))
                    .thenReturn(false)  // Primera llamada: no existe
                    .thenReturn(true);  // Segunda llamada: ya existe
            when(revokedTokenRepository.save(any(RevokedToken.class))).thenReturn(mock(RevokedToken.class));

            // When
            tokenBlackListService.revokeToken(validJti, futureExpirationDate);
            tokenBlackListService.revokeToken(validJti, futureExpirationDate);

            // Then
            verify(revokedTokenRepository, times(2)).existsByJti(validJti);
            verify(revokedTokenRepository, times(1)).save(any(RevokedToken.class));
        }
    }

}
