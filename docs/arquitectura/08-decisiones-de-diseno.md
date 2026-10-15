# Decisiones de diseño

Las decisiones que atraviesan varios dominios. Cada una dice qué se hace, por qué (según lo que muestra el código) y dónde verlo. Las decisiones propias de un dominio están en su archivo de [dominios/](../arquitectura/README.md#dominios).

## Estructura

### Capas con una capa de validación separada

- **Qué:** controller → service → validation → repository, con mappers a mano. Las reglas de negocio viven en clases `*ValidationService`, que lanzan excepciones o devuelven la entidad ya cargada.
- **Por qué:** el service se lee como una lista de pasos, y una regla compartida (por ejemplo, la visibilidad de una campaña) se escribe una sola vez y la usan varios dominios.
- **Dónde:** `validation/campaign/CampaignValidationService.java` (usada por donation, publication y report). Detalle en [Arquitectura](02-arquitectura.md).

### Mappers escritos a mano

- **Qué:** sin MapStruct ni ModelMapper.
- **Por qué:** los DTOs no son copias de la entidad: aplanan relaciones (meta y dueño en la campaña) y reciben datos ya cargados (las categorías), lo que deja explícito qué hace falta cargar antes de mapear.
- **Dónde:** `mapper/`.

## Identidad y autorización

### Quién llama sale del token, nunca del body

- **Qué:** los services obtienen el usuario con `AuthenticatedUserService.getAuthenticatedUserId()`, y en rutas públicas con `findAuthenticatedUserId()`, que devuelve vacío para un anónimo.
- **Por qué:** nadie puede actuar en nombre de otro. El autor de una campaña, una publicación, una donación, un seguimiento o un reporte es siempre quien llama.
- **Dónde:** `security/AuthenticatedUserService.java`.
- **Excepciones hoy:** las cuentas bancarias y las solicitudes de verificación toman el usuario del body o de la ruta. Ver [deuda técnica](deuda-tecnica.md).

### El estado de la cuenta y el rol se releen de la base en cada request

- **Qué:** el filtro JWT carga al usuario en cada request. Si está baneado o suspendido responde `403`, y arma las autoridades (`ROLE_<rol>`) con el rol guardado. Los services que deciden "¿es admin?" también consultan la base.
- **Por qué:** una sanción corta todas las sesiones al instante sin revocar tokens, y vuelve a habilitarlas sola cuando vence. Un cambio de rol se aplica sin esperar a que expire el token. El claim `role` del JWT no se usa para autorizar.
- **Dónde:** `security/JwtAuthenticationFilter.java`, `service/auth/CustomUserDetailsService.java`, `validation/auth/AccountStatusValidationService.java`.

### JWT stateless con revocación por lista negra

- **Qué:** access y refresh tokens se distinguen por el claim `type`. Los tokens invalidados antes de vencer se guardan por su `jti`, con una restricción única. Cada refresh token sirve una sola vez (rotación). Al cambiar email o contraseña, `tokensValidAfter` invalida todos los tokens emitidos antes.
- **Por qué:** no hay sesión en el servidor, pero el logout y la rotación tienen que poder cortar un token. La restricción única resuelve las carreras entre dos refresh simultáneos sin locks.
- **Dónde:** `service/auth/JwtService.java`, `service/auth/TokenBlackListService.java`, `service/auth/AuthenticationService.java`. Detalle en [Seguridad](03-seguridad.md).

## Visibilidad

### Lo que no se puede ver responde igual que lo que no existe

- **Qué:** una campaña `CLOSED` ajena, una campaña de un dueño borrado o sancionado, y un usuario sancionado responden `404` con el mismo mensaje que un id inexistente, salvo para un ADMIN.
- **Por qué:** un tercero no puede distinguir "no existe" de "existe pero está oculto", así que no se entera de la sanción ni del cierre. El código lo dice en sus comentarios ("hidden like a missing one", "the sanction is not revealed").
- **Dónde:** `validation/campaign/CampaignValidationService.java`, `validation/user/UserValidationService.java`, `validation/follower/FollowerValidationService.java`.

### Un criterio de "bloqueado", escrito dos veces

- **Qué:** bloqueado = `banned` o `suspendedUntil` posterior a ahora. En Java es `AccountStatusValidationService.isBlocked`. En JPQL, la constante `UserRepository.NOT_BLOCKED`, que recibe `:now` desde la JVM.
- **Por qué:** los listados filtran en la base y las validaciones puntuales en memoria, con la misma regla. Pasar la hora desde la JVM evita depender del reloj del servidor de base de datos.
- **Dónde:** `validation/auth/AccountStatusValidationService.java`, `repository/user/UserRepository.java`.

### Queries en pares: la del ADMIN y la del resto

- **Qué:** las lecturas tienen una versión completa (ADMIN) y una `*VisibleTo` o `*Visible*` (resto), y el service elige con un booleano.
- **Por qué:** el ADMIN modera, así que ve lo oculto en las lecturas. En las escrituras no tiene excepción (no puede seguir a un usuario sancionado).
- **Dónde:** `repository/campaign/CampaignRepository.java`, `repository/follower/FollowerRepository.java`.

### Ocultar un campo en lugar de cerrar el endpoint

- **Qué:** el listado de donaciones de una campaña es público, pero `transactionId` solo lo ven el dueño, un ADMIN o el propio donante.
- **Dónde:** `service/donation/DonationService.java`.

## Datos

### Borrado lógico, con relaciones tolerantes

- **Qué:** `User`, `Campaign`, `Publication` y `VerificationRequest` se borran con `@SQLDelete` + `@SQLRestriction("deleted_at IS NULL")`. Las relaciones que apuntan a ellas desde registros históricos (donaciones, reportes, publicaciones) usan `@ManyToOne` opcional + `@NotFound(IGNORE)` + `updatable = false`, y una copia de solo lectura de la FK (`campaignId`, `donorUserId`).
- **Por qué:** lo borrado desaparece de toda consulta, pero una donación o un reporte no desaparecen porque se borró su campaña o su autor: la relación queda en `null` y la FK no se pisa en un `UPDATE`. La FK de solo lectura permite filtrar sin hacer join a una tabla con restricción.
- **Dónde:** `entity/donation/Donation.java`, `entity/publication/Publication.java`, `entity/report/Report.java`. Detalle en [Modelo de datos](04-modelo-de-datos.md).

### Borrar una cuenta cierra sus campañas, y loguearse la restaura

- **Qué:** la baja de un usuario cierra sus campañas (no las borra). El login ve las cuentas borradas con una query nativa y las restaura si la contraseña es correcta.
- **Dónde:** `service/campaign/CampaignService.java` (`closeAllByOwner`), `service/auth/AuthenticationService.java`, `repository/user/UserRepository.java` (`findByEmailIncludingDeleted`).

### Montos `BigDecimal`, sumas atómicas y bloqueo pesimista

- **Qué:** los montos son `BigDecimal` con 2 decimales, y `@Digits(13, 2)` coincide con la columna (`precision = 15, scale = 2`). `Goal.amountRaised` es `updatable = false`: solo lo cambia un `UPDATE` atómico al donar, seguido de un `UPDATE` condicional que pasa la meta a `COMPLETED` una sola vez. Las decisiones de estado (cerrar, activar, editar la meta, borrar) bloquean la meta con `PESSIMISTIC_WRITE`.
- **Por qué:** guardar una campaña nunca pisa donaciones concurrentes, y una donación y un cierre simultáneos no se intercalan.
- **Dónde:** `entity/campaign/Goal.java`, `repository/campaign/GoalRepository.java`, `service/donation/DonationService.java`, `service/campaign/CampaignService.java`.

### Historial de estados de campaña

- **Qué:** toda transición de `CampaignStatus`, manual o automática, se registra con quién la hizo. `changedBy = null` significa que la hizo el sistema (por ejemplo, alcanzar la meta).
- **Dónde:** `service/campaign/CampaignStatusHistoryService.java`.

### La base como red contra carreras

- **Qué:** restricciones únicas (seguidor+seguido, `jti` revocado, reportante+entidad, número de cuenta, `transactionId`). `DataIntegrityViolationException` se traduce a `409`.
- **Por qué:** la validación previa en Java no alcanza con requests simultáneos, y la restricción da la última palabra.
- **Dónde:** las entidades, `exception/GlobalExceptionHandler.java`.

### Consultas sin N+1

- **Qué:** los listados traen las relaciones con `JOIN FETCH`, incluido el `@OneToOne` inverso `User.verificationRequest`, que Hibernate cargaría aparte aunque fuera `LAZY`. Las categorías de un listado de campañas se cargan en bloques de 1000 ids, porque SQL Server acepta hasta 2100 parámetros.
- **Dónde:** `repository/campaign/CampaignRepository.java`, `repository/user/UserRepository.java`, `service/campaign/CampaignService.java`.

## Errores

### Excepciones de dominio y un manejador central

- **Qué:** las reglas lanzan excepciones de `exception/`, todas subclases de `MessageException`, y `GlobalExceptionHandler` decide el status y arma `{error, message, timestamp}`.
- **Dónde:** detalle en [Errores y validación](05-errores-y-validacion.md).

## Producto

### Aprobación simulada de donaciones

- **Qué:** una donación se guarda directamente como `COMPLETED`, con un `transactionId` generado. `PENDING`, `FAILED` y `REFUNDED` existen en el enum, pero no se usan.
- **Por qué:** no hay pasarela de pago. El service marca el punto donde se integraría una.
- **Dónde:** `service/donation/DonationService.java`, `entity/donation/DonationStatus.java`.

### Moderación solo por reportes

- **Qué:** un ADMIN no borra publicaciones ni sanciona usuarios desde sus dominios: lo hace resolviendo un reporte con una acción (`CONTENT_REMOVED`, `USER_SUSPENDED`, `USER_BANNED`). Las campañas son la excepción, porque un ADMIN puede editarlas y borrarlas directamente.
- **Dónde:** `service/report/ReportActionService.java`, `validation/campaign/CampaignAuthorizationService.java`.

### Secreto JWT obligatorio y validado al arrancar

- **Qué:** `jwt.secret` sale de `JWT_SECRET` sin valor por defecto, y la aplicación no levanta si está vacío, es un placeholder conocido o tiene menos de 32 bytes.
- **Dónde:** `service/auth/JwtService.java`, `src/main/resources/application.properties`.
