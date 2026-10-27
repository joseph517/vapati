# TODO: emails, confirmación de cuenta y login con Google

Orden sugerido:

1. Infraestructura de mail (con Mailpit en el stack dev)
2. Confirmación de email (depende de 1)
3. Login/registro con Google (independiente, salvo que marca `emailVerified=true`)
4. Notificaciones, evento por evento (depende de 1)

Decisiones pendientes:

- [ ] Qué notificaciones van primero
- [ ] Registro con Google en dos pasos (recomendado) o volver opcionales `phone`/`description` y crear la cuenta directamente
- [ ] Si al cambiar el email en `updateUser` se vuelve a pedir confirmación (recomendado: sí)

---

## 1. Infraestructura de envío de correos

- [ ] Agregar dependencia en `pom.xml`:
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-mail</artifactId>
  </dependency>
  ```
  `spring-boot-starter-thymeleaf` ya está: las plantillas HTML van en `src/main/resources/templates/email/*.html`.
- [ ] Configuración en `application.properties` (y `application-docker.properties`):
  ```properties
  spring.mail.host=${MAIL_HOST:localhost}
  spring.mail.port=${MAIL_PORT:1025}
  spring.mail.username=${MAIL_USERNAME:}
  spring.mail.password=${MAIL_PASSWORD:}
  spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:false}
  spring.mail.properties.mail.smtp.starttls.enable=${MAIL_STARTTLS:false}
  app.mail.from=${MAIL_FROM:no-reply@vapati.local}
  app.frontend-url=${FRONTEND_URL:http://localhost:3000}
  ```
- [ ] Dev: agregar Mailpit a `docker-compose.dev.yml` (`axllent/mailpit`, SMTP 1025, web 8025). Captura los correos, no envía nada real.
- [ ] Prod: elegir proveedor SMTP (Brevo, Resend, SendGrid, Amazon SES; Gmail con "contraseña de aplicación" tiene límites bajos).
- [ ] `service/notification/EmailService`: usa `JavaMailSender` + `TemplateEngine`, método `send(to, subject, template, variables)` con `@Async`.
- [ ] `@EnableAsync` en alguna clase de configuración.

## 2. Confirmación de email al registrarse

No reutilizar `User.verified` (`is_verified`): es la verificación de cuenta por solicitud.

- [ ] Campo nuevo en `User`:
  ```java
  @Column(name = "email_verified", nullable = false, columnDefinition = "bit not null default 0")
  private boolean emailVerified = false;
  ```
  El `default 0` hace falta porque con `ddl-auto=update` SQL Server no puede agregar una columna `NOT NULL` sin default a una tabla con filas.
- [ ] Marcar una sola vez a los usuarios existentes: `UPDATE [user] SET email_verified = 1`.
- [ ] Entidad `entity/auth/EmailVerificationToken`: `id`, `user`, `tokenHash`, `expiresAt`, `usedAt`.
  - Token generado con `SecureRandom` (32 bytes en Base64URL).
  - En la base solo el hash SHA-256.
  - Vencimiento: ~24 h.
- [ ] Repositorio `repository/auth/EmailVerificationTokenRepository`.
- [ ] `UserService.createUser`: después de `save`, crear el token y publicar `UserRegisteredEvent`. El listener manda el correo con el link `${app.frontend-url}/confirm-email?token=...`.
- [ ] `POST /auth/confirm-email {token}`: busca por hash, valida que no esté vencido ni usado, pone `emailVerified=true` y marca `usedAt`.
- [ ] `POST /auth/resend-confirmation {email}`: invalida tokens anteriores y genera uno nuevo. Siempre 204 (no revelar si el email existe). Limitar reenvíos (ej. 1 por minuto por usuario).
- [ ] Login: en `AuthenticationService.authenticate`, después de `validateUserStatus`, si `!user.isEmailVerified()` → 403 "Email not confirmed". Lo mismo en `refreshToken`.
- [ ] Validaciones en `validation/auth/EmailVerificationValidationService`.
- [ ] Rutas nuevas quedan dentro de `/auth/**` (ya pública).
- [ ] Tests en `service/auth/` y `validation/auth/`.

## 3. Registro e inicio de sesión con Google

Enfoque: el frontend usa Google Identity Services (botón "Sign in with Google") y obtiene un ID token; el backend lo verifica y devuelve el `AuthResponse` propio (mismo JWT, refresh token y blacklist). No usar `spring-boot-starter-oauth2-client` (redirecciones + sesión HTTP, choca con `STATELESS`).

- [ ] Google Cloud Console: crear OAuth Client ID tipo "Web application", agregar `http://localhost:3000` y el dominio de prod en *Authorized JavaScript origins*. Guardar como `GOOGLE_CLIENT_ID` (no hace falta client secret).
- [ ] Propiedad `google.client-id=${GOOGLE_CLIENT_ID}`.
- [ ] Dependencia:
  ```xml
  <dependency>
      <groupId>com.google.api-client</groupId>
      <artifactId>google-api-client</artifactId>
      <version><!-- última 2.x --></version>
  </dependency>
  ```
- [ ] `service/auth/GoogleTokenVerifierService`:
  ```java
  private final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
          new NetHttpTransport(), GsonFactory.getDefaultInstance())
          .setAudience(List.of(googleClientId))   // @Value("${google.client-id}")
          .build();

  public GoogleIdToken.Payload verify(String idToken) {
      try {
          GoogleIdToken token = verifier.verify(idToken);   // revisa firma, aud, iss y exp
          if (token == null) throw new InvalidCredentialsException("Invalid Google token");
          return token.getPayload();   // getSubject(), getEmail(), getEmailVerified(), "given_name", "family_name", "picture"
      } catch (GeneralSecurityException | IOException e) {
          throw new InvalidCredentialsException("Invalid Google token");
      }
  }
  ```
- [ ] Cambios de esquema:
  - [ ] `UserInfo.password` nullable (un login normal contra cuenta sin contraseña falla solo con "Invalid email or password").
  - [ ] Columna `google_id` nullable, sin `unique = true` en la anotación. SQL Server permite un solo NULL en un `UNIQUE` común, así que índice filtrado a mano:
    ```sql
    CREATE UNIQUE INDEX ux_user_info_google_id ON user_info(google_id) WHERE google_id IS NOT NULL;
    ```
- [ ] Registro en dos pasos (`phone`, `description`, `userName` y categorías son obligatorios y Google no los da):
  - [ ] `POST /auth/google {idToken}`:
    - existe usuario con ese `googleId` → login, devuelve `AuthResponse`;
    - existe usuario con ese email y Google confirma `email_verified` → vincular `googleId` y login;
    - no existe → `{ registrationRequired: true, registrationToken, email, firstName, lastName, picture }`. `registrationToken`: JWT corto (~10 min, `type=google_registration`) firmado con `JwtService`, con `sub` y email de Google.
  - [ ] `POST /auth/google/register {registrationToken, userName, phone, description, categoryIds}`: crea el usuario reutilizando `UserValidationService`, con `emailVerified=true` y sin contraseña. Devuelve `AuthResponse`.
- [ ] En `/auth/google` repetir lo de `authenticate`: `findByEmailIncludingDeleted`, `validateNotSanctioned`, restaurar cuenta si la borró el propio usuario.
- [ ] (Más adelante) permitir "crear contraseña" desde el perfil para cuentas solo-Google.

## 4. Notificaciones por email

Basadas en eventos, no llamar al mail desde cada servicio:

```java
// en DonationService, después de guardar
eventPublisher.publishEvent(new DonationReceivedEvent(donation.getId()));

// en notification/DonationNotificationListener
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(DonationReceivedEvent event) { ... emailService.send(...) }
```

`AFTER_COMMIT` evita mandar correos si la transacción hace rollback.

Candidatas:

- [ ] Donación recibida
- [ ] Nuevo seguidor
- [ ] Campaña que llegó a la meta
- [ ] Reporte resuelto
- [ ] Baneo / suspensión
- [ ] (Opcional) tabla `notification_preference` para que el usuario desactive avisos
