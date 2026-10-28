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

- **Qué:** una ruta puede colgar de otro recurso y vivir en el dominio dueño de los datos. `GET /api/users/{userId}/supporters` y `GET /api/users/{userId}/support-feed` están en `controller/donation/SupporterController.java`, con `@RequestMapping("/api/users/{userId}")`, igual que los seguidores en `controller/follower/FollowerController.java`. `UserController` no cambia. Los comentarios siguen el mismo criterio: `controller/comment/CommentController.java` atiende `/api/publications/{publicationId}/comments` desde el dominio `comment`, y `PublicationController` no cambia.
- **Por qué:** ningún controller inyecta el service de otro dominio. Los supporters son donaciones agregadas, así que el controller, el service (`SupporterService`) y la regla de orden van en donation, y la ruta queda donde la busca el cliente, bajo el usuario. Los comentarios tienen sus propias reglas, su tabla y su borrado, y `publication` no importa nada de `comment`: si lo hiciera (por ejemplo para contarlos en `PublicationResponseDTO`), los dos dominios dependerían uno del otro.
- **Dónde:** `controller/donation/SupporterController.java`, `service/donation/SupporterService.java`, `controller/comment/CommentController.java`. Specs: `harness/specs/implementados/50-supporters-de-un-creador.md`, `harness/specs/implementados/52-feed-de-apoyos-en-el-perfil.md` y `harness/specs/54-comentarios-en-publicaciones.md`.

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

### El rol ADMIN se comprueba en un solo lugar

- **Qué:** `RoleValidationService.isAdmin(userId)` lee el rol de la base para un `userId` con una sola consulta (`existsByIdAndRole_Name`), y `hasAdminRole(user)` lo lee sobre un usuario ya cargado con su rol, sin consulta extra.
- **Por qué:** antes había cuatro copias de la comprobación, y dominios que dependían de `campaign` solo para saber el rol. Leerlo de la base evita un claim viejo en el token.
- **Dónde:** `validation/common/RoleValidationService.java`.

### JWT stateless con revocación por lista negra

- **Qué:** access y refresh tokens se distinguen por el claim `type`. Los tokens invalidados antes de vencer se guardan por su `jti`, con una restricción única. Cada refresh token sirve una sola vez (rotación). Al cambiar email o contraseña, `tokensValidAfter` invalida todos los tokens emitidos antes.
- **Por qué:** no hay sesión en el servidor, pero el logout y la rotación tienen que poder cortar un token. La restricción única resuelve las carreras entre dos refresh simultáneos sin locks.
- **Dónde:** `service/auth/JwtService.java`, `service/auth/TokenBlackListService.java`, `service/auth/AuthenticationService.java`. Detalle en [Seguridad](03-seguridad.md).

## Visibilidad

### Lo que no se puede ver responde igual que lo que no existe

- **Qué:** una campaña `CLOSED` ajena, una campaña de un dueño borrado o sancionado, y un usuario sancionado responden `404` con el mismo mensaje que un id inexistente, salvo para un ADMIN.
- **Por qué:** un tercero no puede distinguir "no existe" de "existe pero está oculto", así que no se entera de la sanción ni del cierre. El código lo dice en sus comentarios ("hidden like a missing one", "the sanction is not revealed").
- **Dónde:** `validation/campaign/CampaignValidationService.java`, `validation/user/UserValidationService.java`, `validation/follower/FollowerValidationService.java`.

### Un criterio de "sancionado", escrito dos veces

- **Qué:** sancionado = `banned` o `suspendedUntil` posterior a ahora. En Java es `AccountStatusValidationService.isSanctioned`. En JPQL, la constante `UserRepository.NOT_SANCTIONED`, que recibe `:now` desde la JVM. "Sancionado" es el término del proyecto para baneado o con suspensión vigente; "bloqueo" queda para el bloqueo entre usuarios.
- **Por qué:** los listados filtran en la base y las validaciones puntuales en memoria, con la misma regla. Pasar la hora desde la JVM evita depender del reloj del servidor de base de datos.
- **Dónde:** `validation/auth/AccountStatusValidationService.java`, `repository/user/UserRepository.java`.

### Queries en pares: la del ADMIN y la del resto

- **Qué:** las lecturas tienen una versión completa (ADMIN) y una `*VisibleTo` o `*Visible*` (resto), y el service elige con un booleano.
- **Por qué:** el ADMIN modera, así que ve lo oculto en las lecturas. En las escrituras no tiene excepción (no puede seguir a un usuario sancionado).
- **Dónde:** `repository/campaign/CampaignRepository.java`, `repository/follower/FollowerRepository.java`, `repository/comment/CommentRepository.java` (`findAllByPublicationId` y `findVisibleByPublicationId`).

### Ocultar un campo en lugar de cerrar el endpoint

- **Qué:** el listado de donaciones de una campaña es público, pero `transactionId`, el mensaje privado y la respuesta privada del dueño solo los ven el dueño, un ADMIN o el propio donante. Para el resto, un mensaje privado se ve como una donación sin mensaje (`message: null`, `messagePrivate: false`), y una respuesta privada como una donación sin respuesta (`thanksMessage: null`, `thanksPrivate: false`, `thankedAt: null`), para no revelar que hay algo oculto.
- **Excepciones por diseño:** `GET /api/donations/received` no oculta nada: lo llama el receptor, que ve el `transactionId`, el mensaje privado y la respuesta privada de todo lo que recibió, así que no usa `canSeePrivateDonationData`. Las donaciones directas se leen sueltas en el feed de apoyos del receptor, que no usa `canSeePrivateDonationData`: es una vista pública única (ver [Feed de apoyos: la misma vista para todos](#feed-de-apoyos-la-misma-vista-para-todos)).
- **Dónde:** `service/donation/DonationService.java` (`canSeePrivateDonationData`, `getDonationsReceived`). Lo decide el service y no el mapper, porque el mapper no conoce a quien llama.

### Supporters: la identidad es pública, los importes no

- **Qué:** `GET /api/users/{userId}/supporters` es público, también sin token, como las donaciones de una campaña. Cualquiera ve `donorUserId` y `donorUserName`. Solo el propio creador y un ADMIN ven `totalAmount`, `donationsCount` y `lastDonationAt`; el resto los recibe en `null`. `sortBy=lastDonationAt` solo se permite a esos dos (`400` para el resto). Hay un solo endpoint paginado: el "top" es `size=3`.
- **Por qué:** sigue a Ko-fi, cuyo perfil público muestra los primeros supporters sin importes. Ordenar por fecha revelaría quién donó más recientemente, que es el dato oculto; por eso se restringe. El orden por `totalAmount` sí revela quién donó más, y se aceptó como riesgo, como en Ko-fi. Ocultar campos y no cerrar la ruta es el mismo criterio que el `transactionId` y el mensaje privado, y como cada fila es un agregado sin mensajes no aplica `canSeePrivateDonationData`.
- **Donantes inactivos, sin código nuevo:** un donante borrado sale anónimo (`donorUserId` y `donorUserName` en `null`) para todos, también para el ADMIN, como en `DonationMapper.toDTO`. Un donante baneado o suspendido sale normal, como en el listado de donaciones de una campaña. Sus importes siguen sumando: el ranking es un historial.
- **Creador:** mismas reglas que el perfil (`404 User not found` si no existe, está borrado o, para la vista pública, sancionado). Es la primera ruta anónima que distingue un id de usuario existente de uno inexistente; no revela sanciones.
- **Dónde:** `service/donation/SupporterService.java`, `validation/donation/DonationValidationService.java` (`validateSupporterSort`), `mapper/donation/DonationMapper.java` (`toSupporterDTO`). Detalle en [Donation](dominios/donation.md#supporters-de-un-creador). Spec: `harness/specs/implementados/50-supporters-de-un-creador.md`.

### Feed de apoyos: la misma vista para todos

- **Qué:** `GET /api/users/{userId}/support-feed` es público, también sin token, y devuelve la misma vista a todos, incluidos el creador y el ADMIN: una fila por donación `COMPLETED` recibida, sin importes, con el mensaje y la respuesta privados en `null` (sin flag) y con las campañas no públicas (borradas, `CLOSED` o sin `goal`) anonimizadas con `campaignHidden=true`. El orden es fijo, `createdAt` y `id` descendentes. La identidad del donante es pública, como en los supporters.
- **Por qué:** lo privado y el detalle completo ya están en `GET /api/donations/received`, que ve el receptor. Una sola vista pública hace que el contenido no dependa de quien llama. Un DTO propio (`SupportFeedItemDTO`) impide que un campo nuevo de `DonationResponseDTO` se filtre a una ruta pública. La campaña se oculta con la misma regla de `CLOSED` que `findByIdVisibleTo` aplica a quien no es dueño ni ADMIN, así el feed no muestra una campaña que el público no puede abrir.
- **Dónde:** `service/donation/SupporterService.java` (`getSupportFeed`, `hideNonPublicData`), `validation/campaign/CampaignValidationService.java` (`isPublicCampaign`), `validation/donation/DonationValidationService.java` (`isCampaignPublic`). Detalle en [Donation](dominios/donation.md#feed-de-apoyos-de-un-creador). Spec: `harness/specs/52-feed-de-apoyos-en-el-perfil.md`.

### Likes: contador sin vista de ADMIN

- **Qué:** `likesCount` deja afuera los likes de usuarios borrados, baneados o con suspensión vigente, para todos, también para el ADMIN. Las filas se conservan y vuelven a contar si la cuenta se restaura o la sanción termina.
- **Por qué:** no hay par de queries completa/`*VisibleTo` como pide [Queries en pares](#queries-en-pares-la-del-admin-y-la-del-resto): no hay una lista de quién dio like, solo un número, y una vista de ADMIN no agregaría nada. Es la regla de lectura de los usuarios bloqueados (`UserRepository.NOT_BLOCKED` junto con `u.deletedAt IS NULL`).
- **Dónde:** `repository/publication/PublicationLikeRepository.java`. Detalle en [Publication](dominios/publication.md).

### Comentarios: visibles con su publicación, y los borra también el dueño

- **Qué:** un comentario se ve si su autor está activo (o sancionado, si quien llama es ADMIN) y su publicación es visible para quien llama. Lo borran su autor, el autor de la publicación o un ADMIN, y primero se mira la visibilidad y después el permiso: lo que quien llama no ve responde el `404` del comentario, y un comentario visible que no puede borrar responde `403`. Todo lo oculto responde el mismo `404 Comment not found with ID: <id>`, nunca el de la publicación.
- **Por qué:** el comentario se escribe en el espacio del creador, así que el creador puede limpiarlo aunque no lo haya escrito; y el ADMIN modera, como en las lecturas de [Queries en pares](#queries-en-pares-la-del-admin-y-la-del-resto). El listado sí tiene vista de ADMIN (a diferencia de los likes), porque es una lista de personas y no un número. Responder el `404` de la publicación revelaría que el comentario existe.
- **Dónde:** `validation/comment/CommentValidationService.java`, `repository/comment/CommentRepository.java`. Detalle en [Comment](dominios/comment.md).

## Datos

### Borrado lógico, con relaciones tolerantes

- **Qué:** `User`, `Campaign`, `Publication`, `Comment` y `VerificationRequest` se borran con `@SQLDelete` + `@SQLRestriction("deleted_at IS NULL")`. Las relaciones que apuntan a ellas desde registros históricos (donaciones, reportes, publicaciones, comentarios) usan `@ManyToOne` opcional + `@NotFound(IGNORE)` + `updatable = false`, y una copia de solo lectura de la FK (`campaignId`, `donorUserId`, `recipientUserId`, `publicationId`). `Comment.user` sigue ese patrón; `Comment.publication` es LAZY, sin `@NotFound`, y solo se navega cuando la publicación ya resultó visible.
- **Por qué:** lo borrado desaparece de toda consulta, pero una donación o un reporte no desaparecen porque se borró su campaña o su autor: la relación queda en `null` y la FK no se pisa en un `UPDATE`. La FK de solo lectura permite filtrar sin hacer join a una tabla con restricción.
- **Dónde:** `entity/donation/Donation.java`, `entity/publication/Publication.java`, `entity/comment/Comment.java`, `entity/report/Report.java`. Detalle en [Modelo de datos](04-modelo-de-datos.md).
- **Costo:** con Hibernate 6.6, borrar con `repository.delete` una entidad con `@SQLDelete` la deja removida para el contexto de persistencia, y cualquier entidad administrada de la misma transacción que la referencie hace fallar el flush. `CampaignService.closeAndSoftDelete` lo resuelve desacoplando (`detach`) la fila de historial que acaba de escribir. Detalle en [Modelo de datos](04-modelo-de-datos.md#borrado-lógico-soft-delete) y [Recomendaciones de arquitectura](recomendaciones-de-arquitectura.md).

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

- **Qué:** restricciones únicas (seguidor+seguido, `jti` revocado, reportante+entidad, número de cuenta, `transactionId`, publicación+usuario en `publication_like`). Por defecto, `DataIntegrityViolationException` se traduce a `409`. Hay dos excepciones idempotentes que absorben la violación y siguen: el logout (`AuthenticationService.revokeIgnoringDuplicates`: si otro logout revocó el mismo `jti`, ya está revocado) y el like (`PublicationLikeService.likePublication`: si otro request dio el mismo like, el estado final es el mismo y responde `200`).
- **Por qué:** la validación previa en Java no alcanza con requests simultáneos, y la restricción da la última palabra. Los dos métodos idempotentes no son `@Transactional` a propósito: el rollback de la violación queda confinado al `save` o al insert que falla (y, con open-in-view activo, el contexto de persistencia del request se limpia), así el método puede seguir y responder `200`. Dentro de una transacción externa esa violación la marcaría rollback-only y terminaría en `409`. Quien copie el patrón dentro de un método `@Transactional` obtiene el `409` sin aviso.
- **Dónde:** las entidades, `exception/GlobalExceptionHandler.java`, `service/auth/AuthenticationService.java`, `service/publication/PublicationLikeService.java`.

### Consultas sin N+1

- **Qué:** los listados traen las relaciones con `JOIN FETCH`, incluido el `@OneToOne` inverso `User.verificationRequest`, que Hibernate cargaría aparte aunque fuera `LAZY`. Las categorías de un listado de campañas se cargan en bloques de 1000 ids, porque SQL Server acepta hasta 2100 parámetros.
- **El listado de publicaciones:** suma dos consultas fijas por `campaignId` (una sola para un anónimo): el conteo de likes agrupado por publicación y los ids que quien llama marcó. Ninguna usa `IN :ids`, así que no crecen con el listado ni tocan el límite de parámetros.
- **El listado de comentarios:** trae el autor, su `userInfo` y su `verificationRequest` con `JOIN FETCH`, todos a uno, así que Hibernate pagina en SQL. Cada página son dos consultas fijas (la página y el conteo), más las de visibilidad.
- **Dónde:** `repository/campaign/CampaignRepository.java`, `repository/user/UserRepository.java`, `service/campaign/CampaignService.java`, `repository/publication/PublicationLikeRepository.java`, `service/publication/PublicationService.java`, `repository/comment/CommentRepository.java`.

## Errores

### Excepciones de dominio y un manejador central

- **Qué:** las reglas lanzan excepciones de `exception/`, todas subclases de `MessageException`, y `GlobalExceptionHandler` decide el status y arma `{error, message, timestamp}`.
- **Dónde:** detalle en [Errores y validación](05-errores-y-validacion.md).

## Infraestructura

### Dos Dockerfiles, con el mismo `JAVA_VERSION`

- **Qué:** `Dockerfile` construye la imagen de producción (lo publica CI) y `Dockerfile.dev` la del stack de desarrollo con hot reload. Los dos declaran `ARG JAVA_VERSION` con el mismo valor.
- **Por qué:** los dos están en uso y tienen responsabilidades distintas: uno compila el JAR y lo copia a una imagen mínima; el otro solo trae JDK y herramientas, porque el código se monta. Unificarlos en un multi-etapa con `target` se descartó. El mismo `JAVA_VERSION` hace que lo que se desarrolla compile y corra con el mismo update de Java que producción. El costo es repetir el valor, que cubre el procedimiento de actualización.
- **Dónde:** `Dockerfile`, `Dockerfile.dev`. Procedimiento en [Infraestructura](06-infraestructura.md#actualizar-los-tags-fijados). Spec: `specs/59-optimizacion-y-seguridad-docker.md`.

### El perfil `dev` separado de `docker`

- **Qué:** `docker` es la base de los dos stacks (datasource, Hikari, Tomcat, JWT, CORS). Lo propio del desarrollo (SQL formateado, DevTools, logs en `DEBUG`) está en el perfil `dev`, que solo activa el stack de desarrollo (`docker,dev`).
- **Por qué:** antes los dos stacks compartían un perfil con SQL y `DEBUG` encendidos, y producción los heredaba. Las alternativas descartadas fueron pasar esos ajustes por variables de entorno del compose o dejarlo como estaba.
- **Dónde:** `src/main/resources/application-docker.properties`, `src/main/resources/application-dev.properties`.

### La aplicación conecta con un login de mínimo privilegio

- **Qué:** `vapati_app`, con `db_owner` solo sobre `ApiVaPaTiJava`, en lugar de `sa`. Lo crea el propio `command` del servicio `sqlserver` en cada arranque (si existe, solo actualiza la password), y el healthcheck de la base entra con ese login. `sa` queda para inicializar y para los scripts manuales.
- **Por qué:** una inyección o un fallo de la aplicación ya no llega a otras bases ni a la configuración del servidor. Es automático e idempotente porque el servidor de producción solo tiene el compose y el `.env`: no hay dónde montar un script. `db_owner` y no menos, porque `ddl-auto=update` crea y altera tablas. `application.properties` (corrida local fuera de Docker) sigue con `sa`, porque fuera de Compose nadie crea el login.
- **Dónde:** `docker-compose.dev.yml`, `docker-compose.prod.yml`, `application-docker.properties`. Detalle en [Seguridad](03-seguridad.md#contenedores-y-base-de-datos).

### Alpine, usuario no root y tags exactos

- **Qué:** todas las etapas sobre `eclipse-temurin:<versión>-{jdk,jre}-alpine`, con Maven a través del wrapper (sin la imagen `maven:`). La imagen de producción corre como `app`, con sistema de archivos de solo lectura, `no-new-privileges` y sin capacidades. Las imágenes base van por tag exacto, sin digest.
- **Por qué:** la base Ubuntu y la imagen `maven:` aportaban casi todas las vulnerabilidades del sistema operativo (el escaneo de `runtime` pasó de 56 a 3 excepciones, ver [Infraestructura](06-infraestructura.md#escanear-con-trivy)), y la versión de Maven queda definida en un solo lugar. Un digest exige editarlo a mano en cada actualización y no tiene Dependabot en este repo; un tag flotante cambia la imagen sin que nadie lo decida. El desarrollo sigue como root, con `no-new-privileges` y sin capacidades, porque el volumen `maven-cache-dev` es de root.
- **Dónde:** `Dockerfile`, `Dockerfile.dev`, los dos compose.

### SQL Server con `max server memory`

- **Qué:** `sp_configure 'max server memory (MB)', 512` en el `command` de `sqlserver`, más `MSSQL_MEMORY_LIMIT_MB=1024`, en los dos compose.
- **Por qué:** sin tope, el buffer pool crece hasta ocupar la memoria disponible. Cada palanca acota una parte distinta del proceso, así que van las dos. No se usa `mem_limit` en el contenedor de la base ni `MSSQL_PID=Express`: SQL Server en Linux no arranca con un límite menor a 2 GB.
- **Dónde:** `docker-compose.dev.yml`, `docker-compose.prod.yml`.

### El hot reload lanza `java -cp`, no `spring-boot:run`

- **Qué:** `scripts/dev-hot-reload.sh` calcula el classpath con Maven (`dependency:build-classpath`, en la misma invocación que compila) y arranca la JVM directo con `java -cp`.
- **Por qué:** `spring-boot:run` dejaba una JVM de Maven viva además de la de la aplicación (unos 185 MB de RSS). Con `java -cp` hay una sola JVM, con el debug JDWP y DevTools igual que antes, porque `target/classes` sigue en el classpath como directorio.
- **Dónde:** `scripts/dev-hot-reload.sh`. Detalle en [Infraestructura](06-infraestructura.md#hot-reload-en-desarrollo).

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

- **Qué:** el mensaje del donante, la respuesta del receptor, el mensaje predeterminado y el texto de un comentario se validan con el mismo `TextValidationService.normalizeRequiredText`: `trim`, blanco → `400 <campo> cannot be blank`, más de 280 → `400 <campo> must be at most 280 characters`.
- **Por qué:** la usan tres dominios (`donation`, `user` y `comment`), y lo compartido va en `validation/common/`. Así no hay tres copias de la misma regla.
- **Dónde:** `validation/common/TextValidationService.java`.

### Moderación solo por reportes

- **Qué:** un ADMIN no borra publicaciones ni sanciona usuarios desde sus dominios: lo hace resolviendo un reporte con una acción (`CONTENT_REMOVED`, `USER_SUSPENDED`, `USER_BANNED`). Hay tres excepciones, donde un ADMIN actúa directamente: las campañas, que puede editar y borrar; la respuesta a una donación, que puede borrar (`DonationValidationService.validateIsRecipientOrAdmin`); y los comentarios, que puede borrar (`CommentValidationService.validateAndGetDeletableComment`). Las publicaciones y las sanciones siguen pasando solo por reportes.
- **Dónde:** `service/report/ReportActionService.java`, `validation/campaign/CampaignAuthorizationService.java`, `validation/donation/DonationValidationService.java`, `validation/comment/CommentValidationService.java`.

### Secreto JWT obligatorio y validado al arrancar

- **Qué:** `jwt.secret` sale de `JWT_SECRET` sin valor por defecto, y la aplicación no levanta si está vacío, es un placeholder conocido o tiene menos de 32 bytes.
- **Dónde:** `service/auth/JwtService.java`, `src/main/resources/application.properties`.
