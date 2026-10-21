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

### La ruta no decide el dominio del código

- **Qué:** una ruta puede colgar de otro recurso y vivir en el dominio dueño de los datos. `GET /api/users/{userId}/supporters` está en `controller/donation/SupporterController.java`, con `@RequestMapping("/api/users/{userId}")`, igual que los seguidores en `controller/follower/FollowerController.java`. `UserController` no cambia.
- **Por qué:** ningún controller inyecta el service de otro dominio. Los supporters son donaciones agregadas, así que el controller, el service (`SupporterService`) y la regla de orden van en donation, y la ruta queda donde la busca el cliente, bajo el usuario.
- **Dónde:** `controller/donation/SupporterController.java`, `service/donation/SupporterService.java`. Spec: `specs/50-supporters-de-un-creador.md`.

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

- **Qué:** el listado de donaciones de una campaña es público, pero `transactionId`, el mensaje privado y la respuesta privada del dueño solo los ven el dueño, un ADMIN o el propio donante. Para el resto, un mensaje privado se ve como una donación sin mensaje (`message: null`, `messagePrivate: false`), y una respuesta privada como una donación sin respuesta (`thanksMessage: null`, `thanksPrivate: false`, `thankedAt: null`), para no revelar que hay algo oculto.
- **Excepciones por diseño:** `GET /api/donations/received` no oculta nada: lo llama el receptor, que ve el `transactionId`, el mensaje privado y la respuesta privada de todo lo que recibió, así que no usa `canSeePrivateDonationData`. Las donaciones directas no tienen ninguna lectura pública ni de terceros, por eso no hay nada que ocultar en otra ruta.
- **Dónde:** `service/donation/DonationService.java` (`canSeePrivateDonationData`, `getDonationsReceived`). Lo decide el service y no el mapper, porque el mapper no conoce a quien llama.

### Supporters: la identidad es pública, los importes no

- **Qué:** `GET /api/users/{userId}/supporters` es público, también sin token, como las donaciones de una campaña. Cualquiera ve `donorUserId` y `donorUserName`. Solo el propio creador y un ADMIN ven `totalAmount`, `donationsCount` y `lastDonationAt`; el resto los recibe en `null`. `sortBy=lastDonationAt` solo se permite a esos dos (`400` para el resto). Hay un solo endpoint paginado: el "top" es `size=3`.
- **Por qué:** sigue a Ko-fi, cuyo perfil público muestra los primeros supporters sin importes. Ordenar por fecha revelaría quién donó más recientemente, que es el dato oculto; por eso se restringe. El orden por `totalAmount` sí revela quién donó más, y se aceptó como riesgo, como en Ko-fi. Ocultar campos y no cerrar la ruta es el mismo criterio que el `transactionId` y el mensaje privado, y como cada fila es un agregado sin mensajes no aplica `canSeePrivateDonationData`.
- **Donantes inactivos, sin código nuevo:** un donante borrado sale anónimo (`donorUserId` y `donorUserName` en `null`) para todos, también para el ADMIN, como en `DonationMapper.toDTO`. Un donante baneado o suspendido sale normal, como en el listado de donaciones de una campaña. Sus importes siguen sumando: el ranking es un historial.
- **Creador:** mismas reglas que el perfil (`404 User not found` si no existe, está borrado o, para la vista pública, sancionado). Es la primera ruta anónima que distingue un id de usuario existente de uno inexistente; no revela sanciones.
- **Dónde:** `service/donation/SupporterService.java`, `validation/donation/DonationValidationService.java` (`validateSupporterSort`), `mapper/donation/DonationMapper.java` (`toSupporterDTO`). Detalle en [Donation](dominios/donation.md#supporters-de-un-creador). Spec: `specs/50-supporters-de-un-creador.md`.

## Datos

### Borrado lógico, con relaciones tolerantes

- **Qué:** `User`, `Campaign`, `Publication` y `VerificationRequest` se borran con `@SQLDelete` + `@SQLRestriction("deleted_at IS NULL")`. Las relaciones que apuntan a ellas desde registros históricos (donaciones, reportes, publicaciones) usan `@ManyToOne` opcional + `@NotFound(IGNORE)` + `updatable = false`, y una copia de solo lectura de la FK (`campaignId`, `donorUserId`, `recipientUserId`).
- **Por qué:** lo borrado desaparece de toda consulta, pero una donación o un reporte no desaparecen porque se borró su campaña o su autor: la relación queda en `null` y la FK no se pisa en un `UPDATE`. La FK de solo lectura permite filtrar sin hacer join a una tabla con restricción.
- **Dónde:** `entity/donation/Donation.java`, `entity/publication/Publication.java`, `entity/report/Report.java`. Detalle en [Modelo de datos](04-modelo-de-datos.md).

### Agregados que atraviesan el borrado: SQL nativo con el orden escrito

- **Qué:** los supporters suman las donaciones `COMPLETED` que recibió el creador (`recipient_user_id`): las directas y las de sus campañas en cualquier estado, también las borradas, y las de donantes borrados. La agregación la hace la base, con dos queries nativas (`findSupportersByCreatorId` y `countSupportersByCreatorId`) que llevan el `ORDER BY` (un `CASE` por campo y dirección, desempate por `donor_user_id`) y el `OFFSET/FETCH` escritos, sin pasar un `Pageable`.
- **Por qué:** el dinero se recibió aunque la campaña o el donante se hayan borrado, y JPQL aplicaría el `@SQLRestriction`. Filtrar por `recipient_user_id` y no por un `JOIN` con `campaign` es lo que permite sumar las donaciones directas, que no tienen campaña. Agregar en Java traería todas las donaciones para devolver una página. Con el orden escrito en la query, el desempate es fijo y no depende de cómo Spring Data ordena una query nativa. El costo: las queries nativas no se validan al arrancar ni tienen tests unitarios, así que un error de SQL aparece recién al llamarlas.
- **Dónde:** `repository/donation/DonationRepository.java`. Detalle en [Modelo de datos](04-modelo-de-datos.md#leer-a-través-del-borrado).

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

### Agradecer una donación: respuesta única y mensaje predeterminado por usuario

- **Qué:** el receptor de una donación (el dueño de la campaña, o el usuario de una donación directa) responde una sola vez a cada donación (`thanksMessage`, `thanksPrivate`, `thankedAt` en la propia `Donation`). La respuesta no se edita: un segundo `POST` responde `409`, y para cambiarla hay que borrarla y volver a escribirla. La borran el receptor o un ADMIN; la escribe solo el receptor. Aparte, cada usuario tiene un mensaje predeterminado (`UserInfo.autoThanksMessage`) que viaja solo en la respuesta de `POST /api/donations` y no se copia en la donación.
- **Por qué:** sigue el comportamiento de Ko-fi verificado en el cliente: sin botón de editar, con borrar y volver a responder, privacidad propia de la respuesta y mensaje predeterminado por creador. La relación es 1 a 1, así que basta con columnas en `Donation`, sin una entidad aparte. El ADMIN modera borrando, no escribiendo en nombre del receptor. El mensaje predeterminado no se guarda en la donación porque solo sirve como confirmación al donar; guardarlo en `DonationResponseDTO` lo dejaría vacío en todas las demás lecturas.
- **Dónde:** `service/donation/DonationService.java` (`thankDonation`, `deleteDonationThanks`, `createDonation`), `validation/donation/DonationValidationService.java`, `service/user/UserService.java`. Detalle en [Donation](dominios/donation.md) y [User](dominios/user.md). Spec: `specs/49-agradecer-una-donacion.md`.

### Donación directa: campaña opcional y `recipient` siempre presente

- **Qué:** un usuario puede donar directo a otro con el mismo `POST /api/donations`, enviando `recipientUserId` en lugar de `campaignId` (exactamente uno; la regla vive en `DonationValidationService.validateInput`, no en el DTO). `Donation.campaign` pasa a ser opcional y se suma `Donation.recipient`, siempre presente: el dueño de la campaña en una donación a campaña, el usuario elegido en una directa. Una donación directa no toca ninguna campaña (ni `Goal`, ni el recaudado, ni el historial de estados). Responder y borrar la respuesta, los supporters y `GET /api/donations/received` trabajan sobre `recipientUserId`, así que sirven igual a los dos tipos. Los supporters filtran por `recipient_user_id` y no por un `JOIN` con `campaign`.
- **Por qué:** un solo historial en una sola tabla, en lugar de un dominio aparte, evita duplicar el mensaje, el agradecimiento, los supporters y los listados. Un solo endpoint mantiene el contrato de los clientes actuales. Se exige el receptor en toda fila (con un script que rellena las existentes con el dueño de su campaña) para que las consultas por receptor no dependan de si hay campaña.
- **Un ADMIN no recibe donaciones directas** (`400 Cannot donate to an admin`; `404` contradiría al perfil, que muestra a los ADMIN). Es una decisión de producto de este momento: la idea es que el ADMIN deje de ser un usuario común (sin campañas ni donaciones), pero nada en el código lo prepara para eso. Mientras tanto, un ADMIN dueño de una campaña sigue recibiendo donaciones a esa campaña, y un ADMIN puede donar. Un receptor inexistente, borrado, baneado o suspendido responde `404 User not found`, también si dona un ADMIN, para no revelar sanciones.
- **Costo:** `ddl-auto=update` no puede volver nullable a `campaign_id` ni exigir `NOT NULL` en `recipient_user_id` sobre filas existentes, así que hace falta el script manual `scripts/migrate-donation-recipient.sql`, que se corre con la app detenida (ver [Modelo de datos](04-modelo-de-datos.md#migración-manual-de-donation)).
- **Dónde:** `entity/donation/Donation.java`, `validation/donation/DonationValidationService.java`, `service/donation/DonationService.java`, `repository/donation/DonationRepository.java`, `scripts/migrate-donation-recipient.sql`. Detalle en [Donation](dominios/donation.md). Spec: `specs/51-donacion-directa-al-creador.md`.

### Una regla de texto obligatorio compartida

- **Qué:** el mensaje del donante, la respuesta del receptor y el mensaje predeterminado se validan con el mismo `TextValidationService.normalizeRequiredText`: `trim`, blanco → `400 <campo> cannot be blank`, más de 280 → `400 <campo> must be at most 280 characters`.
- **Por qué:** la usan dos dominios (`donation` y `user`), y lo compartido va en `validation/common/`. Así no hay tres copias de la misma regla.
- **Dónde:** `validation/common/TextValidationService.java`.

### Moderación solo por reportes

- **Qué:** un ADMIN no borra publicaciones ni sanciona usuarios desde sus dominios: lo hace resolviendo un reporte con una acción (`CONTENT_REMOVED`, `USER_SUSPENDED`, `USER_BANNED`). Las campañas son la excepción, porque un ADMIN puede editarlas y borrarlas directamente.
- **Dónde:** `service/report/ReportActionService.java`, `validation/campaign/CampaignAuthorizationService.java`.

### Secreto JWT obligatorio y validado al arrancar

- **Qué:** `jwt.secret` sale de `JWT_SECRET` sin valor por defecto, y la aplicación no levanta si está vacío, es un placeholder conocido o tiene menos de 32 bytes.
- **Dónde:** `service/auth/JwtService.java`, `src/main/resources/application.properties`.
