# Recomendaciones de arquitectura

Propuestas de mejora que surgieron al revisar el código, ordenadas por prioridad. **Esto no describe lo que hace el código**: son propuestas, y algunas pueden no estar aplicadas. Cada una dice de qué revisión salió y cuándo. Cuando el código resuelve una recomendación, se borra de este archivo.

Las rutas `.java` son relativas a `src/main/java/com/vaPaTi/vaPaTi/`, como en el resto de la documentación.

## Prioridad alta

### Producción en Azure conecta como administrador del servidor y no usa el compose endurecido

- **Origen:** revisión final del spec 59 (`specs/59-optimizacion-y-seguridad-docker.md`), 2026-10-22. No es una recomendación del architect: sale de la comprobación del orquestador con `az` CLI, confirmada con el usuario.
- **Evidencia:** producción corre en Azure Container Apps (`vapati-api`, grupo de recursos `vapati-rg`), con la imagen `docker.io/joseph517/vapati:latest`, sin `SPRING_PROFILES_ACTIVE` (rige `application.properties`, pisado por `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` y por los secretos `db-password` y `jwt-secret`), contra Azure SQL (`vapati-sql-9137e1`), y conecta como el login de administrador del servidor (`vapatiadmin`). `docker-compose.prod.yml` y el perfil `docker` no participan.
- **Problema:** el login mínimo `vapati_app` y el endurecimiento del spec 59 (usuario no root, `read_only`, `cap_drop`, `.env` fuera del contexto de build) protegen el despliegue con Compose, que no es el que corre hoy. La aplicación en producción real tiene los privilegios de administrador del servidor de base de datos.
- **Propuesta:** crear en Azure SQL un usuario de base contenido (`CREATE USER ... WITH PASSWORD`) con el mínimo privilegio que necesita la aplicación (el análogo de `vapati_app`, que hoy es `db_owner` de `ApiVaPaTiJava` porque `ddl-auto=update` crea y altera tablas), apuntar `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` a él, y documentar el despliegue real en Azure en `DOCKER-HUB-DEPLOY.md` y en [Infraestructura](06-infraestructura.md).
- **Por qué acá:** es la única diferencia entre lo que se endureció y lo que corre. Cambiar el login no toca el código.
- **Costo y riesgo:** sin cambios de código. En operación: hay que crear el usuario en Azure SQL y actualizar el nombre de usuario y el secreto de la Container App; un error en cualquiera de los dos deja la aplicación sin conexión a la base hasta corregirlo.
- **Migración:** (1) crear el usuario contenido en Azure SQL; (2) probar la conexión con ese usuario; (3) cambiar `SPRING_DATASOURCE_USERNAME` y el secreto de la password en la Container App; (4) verificar el arranque y un login de la API; (5) documentar el procedimiento.

## Prioridad media

### No exponer el `.env` del host dentro de `app_vapati_dev`

- **Origen:** revisión final del spec 59, 2026-10-22.
- **Estado:** **aplicada en parte** en esta rama (commit `2d63c2c`): `docker-compose.dev.yml` monta `/dev/null` sobre `/workspace/.env`. Queda pendiente lo que no cubre esa máscara.
- **Evidencia:** `app-dev` monta la raíz del repo en `/workspace` (solo lectura) para que el script lea `pom.xml`; ahí también está el `.env`, con `DB_PASSWORD` (la de `sa`) y `DB_APP_PASSWORD`.
- **Problema:** cualquier proceso del contenedor, incluidas las dependencias de la aplicación, podría leer las passwords del host, que el contenedor no necesita.
- **Propuesta:** la máscara de `/workspace/.env` (hecha). Pendiente: un `.env.production` u otro archivo con secretos en la raíz sigue visible dentro del contenedor.
- **Por qué acá:** el script solo lee `/workspace/pom.xml`, así que ocultar el resto no cuesta funcionalidad.
- **Costo y riesgo:** bajo. La máscara exige que `.env` exista en el host: enmascarar un archivo inexistente hace fallar el arranque del contenedor.
- **Migración:** la máscara ya está. Para lo pendiente, enmascarar los otros archivos de secretos de la misma forma, o dejar de montar la raíz completa si el script puede leer el pom de otro modo.

### El hot reload no recarga la app cuando se crea un paquete nuevo

- **Origen:** revisión final del spec 54 (`harness/specs/54-comentarios-en-publicaciones.md`), 2026-10-27.
- **Evidencia:** en tres pasos del spec 54 que creaban un paquete nuevo bajo `src/main/java` (`entity/comment`, `validation/comment`) el stack quedó `healthy` con la versión vieja: `Comment.class` compilado, pero sin `target/classes/.reloadtrigger` nuevo ni un `Started VaPaTiApplication` posterior, y el único remedio fue reiniciar `app-dev` a mano. Un paso que creó `dtos/comment` sí recargó. En `scripts/dev-hot-reload.sh`, `handle_event` acepta eventos `ISDIR`, así que el fallo está entre la compilación y el `touch` del trigger, o en la pérdida de los eventos de archivos creados dentro de un directorio antes de que `inotifywait -r` le ponga su watch.
- **Problema:** el stack queda `healthy` con código viejo, sin ningún aviso (no existe `/tmp/hot-reload/failed`), y quien prueba lo hace contra la versión anterior. Va a pasar en cada dominio nuevo que cree paquetes.
- **Propuesta:** al recibir `CREATE,ISDIR` dentro de `src/main/java`, hacer un `find <dir> -name '*.java' -newer <marca>` y marcar el cambio de fuentes (cubre la carrera de inotify); y que, tras compilar, el script compruebe que cada `.java` tocado del lote tiene su `.class` y, si no, cree `/tmp/hot-reload/failed` con un banner "RELOAD INCOMPLETE" para que el healthcheck lo delate.
- **Por qué acá:** el desarrollo y la QA por paso dependen del hot reload, y hoy el fallo es silencioso.
- **Costo y riesgo:** chico (bash, unas 20 líneas). Riesgo de recompilar de más. Cambiar `scripts/dev-hot-reload.sh` exige reconstruir la imagen de `app-dev` (`--build`).
- **Migración:** (1) reproducir creando un paquete con una clase; (2) agregar el rescan en `handle_event`; (3) agregar la comprobación posterior a la compilación; (4) reconstruir `app-dev` y repetir la reproducción.

## Prioridad baja

### Que el borrado lógico deje de depender de la semántica de `EntityManager.remove`

- **Origen:** revisión final del spec 59, 2026-10-22.
- **Evidencia:** las entidades con `@SQLDelete` (`entity/campaign/Campaign.java`, `Goal`, `entity/publication/Publication.java`) se borran con `repository.delete`. `service/campaign/CampaignService.java` (`closeAndSoftDelete`) necesita un `entityManager.detach` manual sobre la fila de historial que acaba de escribir.
- **Problema:** Hibernate 6.6 trata una entidad con `@SQLDelete` borrada como removida, así que cualquier entidad administrada de la misma transacción que la referencie hace fallar el flush (un `500`). Cada flujo nuevo que escriba una fila relacionada y después borre lógicamente va a chocar con lo mismo, y solo un test de integración contra SQL Server real lo detecta.
- **Propuesta:** el `@SoftDelete` nativo de Hibernate (6.4 en adelante), o una operación de dominio explícita (por ejemplo `markDeleted()`, que fija `deletedAt`) combinada con el `@SQLRestriction` actual, en lugar de `repository.delete`.
- **Por qué acá:** hoy el borrado lógico depende de un detalle de implementación de Hibernate que cambió entre versiones; ya hay un caso resuelto a mano.
- **Costo y riesgo:** medio. Toca entidades y filtros, y tiene que conservar las columnas `deleted_at`.
- **Migración:** (1) un test de integración por cada camino de borrado lógico; (2) migrar `Campaign` y `Goal` y quitar el `detach`; (3) después `Publication`.

### Quitar el `hibernate.dialect` explícito y silenciar los avisos de arranque

- **Origen:** revisión final del spec 59, 2026-10-22.
- **Estado:** la parte del dialecto **ya está hecha** en esta rama (commit `2d63c2c`): ningún archivo de propiedades lo fija. Queda abierta la otra mitad.
- **Evidencia:** al arrancar, Spring Security avisa que el bean `UserDetailsService` se ignora porque existe un bean `AuthenticationProvider` (`service/auth/CustomUserDetailsService.java` es un `@Service` que implementa `UserDetailsService`; `security/SecurityConfig.java` lo pasa a mano al `DaoAuthenticationProvider` y `security/JwtAuthenticationFilter.java` lo inyecta directamente).
- **Problema:** un aviso en cada arranque que oculta los avisos que sí importan.
- **Propuesta:** revisar si hace falta que `CustomUserDetailsService` implemente `UserDetailsService` y quede registrado como bean de ese tipo, o dejarlo sin la interfaz, de modo que Spring Security no tenga un bean que ignorar.
- **Por qué acá:** el filtro JWT y el login usan la clase por inyección directa, así que el aviso se puede resolver sin cambiar cómo se cargan los usuarios; hay que confirmar que nada la use por la interfaz antes de tocarla.
- **Costo y riesgo:** bajo.
- **Migración:** (1) comprobar los usos de la interfaz; (2) ajustar `CustomUserDetailsService` o `SecurityConfig`; (3) verificar que el aviso desaparece y que el login y el filtro siguen igual.

### Un tipo de "vista pública" para las lecturas de donaciones sueltas

- **Origen:** revisión final del spec 52 (`harness/specs/52-feed-de-apoyos-en-el-perfil.md`), 2026-10-26.
- **Evidencia:** `service/donation/DonationService.java` (`canSeePrivateDonationData` y el paso 5 de `getDonationsByCampaign`, que anula `transactionId`, `message` y `thanks*` sobre `DonationResponseDTO`) y `service/donation/SupporterService.java` (`hideNonPublicData`, que anula `message`, `thanksMessage`, `thankedAt` y la campaña sobre `SupportFeedItemDTO`) repiten la misma regla "privado → `null`, sin flag" en dos sitios con dos DTOs.
- **Problema:** la regla de privacidad del mensaje y de la respuesta vive en dos servicios. Un tercer campo privado, o una tercera lectura, obliga a acordarse de tocar los dos.
- **Propuesta:** un helper de dominio en `validation/donation/DonationValidationService.java` (por ejemplo `publicMessage(Donation)` y `publicThanks(Donation)`, que devuelvan el texto o `null`) usado por los dos servicios: una sola fuente de verdad, sin una capa nueva.
- **Por qué acá:** con los likes en publicaciones (S6) y posibles feeds futuros, el dominio donation ya tiene seis lecturas y dos formas de ocultar.
- **Costo y riesgo:** chico. El riesgo es cambiar la forma de `GET /api/donations/campaign/{id}`, que además pone `messagePrivate=false`; por eso el helper solo debe devolver el texto y no tocar flags.
- **Migración:** (1) agregar los helpers con tests; (2) usarlos en `hideNonPublicData`; (3) usarlos en `getDonationsByCampaign`; suite verde en cada paso.

### Corregir el comentario engañoso sobre `getReferenceById`

- **Origen:** revisión final del spec 54 (`harness/specs/54-comentarios-en-publicaciones.md`), 2026-10-27.
- **Estado:** **aplicada en parte** en la rama del spec 54: `service/comment/CommentService.java` ya dice que `getReferenceById` devuelve un proxy. Queda `service/publication/PublicationLikeService.java` (`likePublication`).
- **Evidencia:** el comentario de `PublicationLikeService` dice "getReferenceById avoids a SELECT of the user, the JWT filter already loaded it".
- **Problema:** atribuye el ahorro del `SELECT` al filtro JWT, cuando lo evita `getReferenceById` porque devuelve un proxy. Es un patrón que se replica por copia y el próximo dominio lo va a copiar otra vez.
- **Propuesta:** "getReferenceById returns a proxy: no SELECT of the caller, who the token already proved exists".
- **Por qué acá:** la misma frase ya se copió una vez de likes a comentarios.
- **Costo y riesgo:** una línea de comentario, sin riesgo.
- **Migración:** un commit `refactor:` con ese archivo.

### Validar `page * size` en `SortValidationService`

- **Origen:** revisión final del spec 52, 2026-10-26 (el QA lo marcó como candidato a un spec común).
- **Evidencia:** `validation/common/SortValidationService.java` valida `page >= 0` y `1 <= size <= 100`, pero no el producto. `GET /api/users/{userId}/support-feed?page=2147483647&size=100` responde `500 Internal server error` (`InvalidDataAccessApiUsageException`: el offset supera `Integer.MAX_VALUE`). `GET /api/donations/received` hace lo mismo; `/supporters` responde `200` vacío porque pagina con una query nativa.
- **Problema:** un parámetro del cliente produce un `500` en los listados paginados con `Pageable`.
- **Propuesta:** un solo guard en `SortValidationService`, en el helper privado `validatePageAndSize` que comparten los dos `validateAndGetPageable` (con y sin orden), que rechace con `400` un `page * size` (calculado como `long`) mayor que `Integer.MAX_VALUE`. Un único cambio cierra el caso en `/support-feed`, `/received` y los demás listados.
- **Por qué acá:** todos los controllers paginados pasan por esa clase.
- **Costo y riesgo:** chico. Hay que elegir el mensaje del `400` y revisar los tests de `SortValidationService`.
- **Migración:** (1) agregar la regla y su mensaje con tests; (2) comprobar a mano `/received` y `/support-feed` con el valor extremo; (3) documentar el mensaje en [Errores y validación](05-errores-y-validacion.md).

### Helper compartido para "insertar ignorando el duplicado"

- **Origen:** revisión final del spec 53 (`harness/specs/53-likes-en-publicaciones.md`), 2026-10-26.
- **Evidencia:** `service/auth/AuthenticationService.java` (`revokeIgnoringDuplicates`) y `service/publication/PublicationLikeService.java` (`likePublication`) repiten el mismo patrón: capturar `DataIntegrityViolationException` al insertar, fuera de una transacción externa (`likePublication` comprueba antes con `existsByPublication_IdAndUser_Id`; el logout comprueba dentro de `TokenBlackListService.revokeToken`). El spec 54 o futuros likes (S7) podrían sumar un tercero.
- **Problema:** la regla "no ser `@Transactional` para que el rollback quede en el `save`" vive en comentarios repetidos. Quien copie el patrón dentro de un método `@Transactional` obtiene un `409` sin aviso.
- **Propuesta:** cuando aparezca el tercer uso, un método utilitario `saveIgnoringDuplicate(Runnable/Supplier)` en un componente común, con el comentario del límite transaccional en un solo lugar.
- **Por qué acá:** el proyecto documenta en [Decisiones de diseño](08-decisiones-de-diseno.md#la-base-como-red-contra-carreras) que la restricción única es la red, y ya hay dos excepciones idempotentes. Con dos usos todavía no paga.
- **Costo y riesgo:** chico. El riesgo es meterlo en un bean con proxy transaccional y cambiar el comportamiento.
- **Migración:** (1) extraer el helper con su test unitario; (2) migrar `likePublication`; (3) migrar `revokeIgnoringDuplicates`; cada paso con su clase de test.

### Un único punto para "oculto salvo para ADMIN" sobre usuarios sancionados

- **Origen:** revisión final del spec 58 (`harness/specs/58-sancion-y-rol-admin-unificados.md`), 2026-10-27.
- **Evidencia:** el patrón `isSanctioned(user) && !isAdmin(caller)` (o `callerIsAdmin || !isSanctioned`) está repetido en `validation/report/ReportValidationService.java` (`validateEntityExists`, rama `USER`), `validation/comment/CommentValidationService.java` (`validateAndGetVisibleComment`), `validation/follower/FollowerValidationService.java` (5 filtros, con un `callerIsAdmin` precalculado), `service/user/UserService.java` (`getUserById`) y `service/donation/SupporterService.java` (`privileged || validateProfileNotSanctioned`).
- **Problema:** hoy la regla es una sola, pero cada llamador decide por su cuenta el orden de evaluación, de modo que la consulta del rol solo corra si hace falta. El spec 57 va a sumar el bloqueo entre usuarios a esos mismos sitios, y cada uno puede combinar las dos reglas distinto.
- **Propuesta:** un método de visibilidad en `validation/common`, por ejemplo `UserVisibilityService.isHiddenFrom(User target, Long callerId)`, que encapsule "sancionado y no ADMIN" (y, con el 57, "bloqueado entre usuarios"), con la evaluación perezosa del rol. Es una Specification en pequeño: una regla con nombre, reutilizable. Conviene evaluarla en el diseño del spec 57.
- **Por qué acá:** el 57 y S11 (mensajes) van a combinar los dos criterios en las mismas clases. Tener un solo punto evita que la combinación diverja.
- **Costo y riesgo:** chico (5 o 6 llamadores, tests que cambian de mock). El riesgo es perder el atajo "no consultar el rol si no está sancionado": hay que conservarlo y probarlo con `never()`.
- **Migración:** (1) evaluarlo en el diseño del spec 57, no antes; (2) crear el método con sus tests; (3) migrar los llamadores de un dominio por paso; (4) borrar los patrones inline.
