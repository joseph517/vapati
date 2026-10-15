# Visión general

## Qué es VaPaTi

VaPaTi es el backend, una API REST, de una plataforma de donaciones. Un usuario crea **campañas** con una meta de recaudación y las acompaña con **publicaciones** de avance. Otros usuarios **donan** a esas campañas y **siguen** a quienes les interesan. Los administradores **moderan** el contenido a partir de **reportes**, **sancionan** usuarios y aprueban **solicitudes de verificación** de cuenta.

El cobro no es real: una donación se aprueba sola en el momento (aprobación simulada). Las **cuentas bancarias** que un usuario vincula se guardan y validan, pero no se integran con ningún medio de pago.

## Actores

| Actor | Cómo se identifica | Qué puede hacer |
|---|---|---|
| **Visitante** | Sin token | Registrarse, iniciar sesión, ver las categorías, y listar y ver campañas visibles con sus publicaciones, sus donaciones y sus estadísticas. |
| **USER** | JWT con rol `USER` | Todo lo del visitante, más: gestionar su perfil, crear y gestionar sus campañas, publicar en ellas, donar, seguir usuarios, vincular cuentas bancarias, reportar contenido y pedir la verificación de su cuenta. |
| **ADMIN** | JWT con rol `ADMIN` | Todo lo anterior, más: listar usuarios, gestionar categorías, revisar y resolver reportes (con sanciones), procesar solicitudes de verificación y ver lo que está oculto para el resto (campañas `CLOSED` ajenas y usuarios sancionados). |

Las rutas públicas están definidas en `security/SecurityConfig.java`. El detalle está en [Seguridad](03-seguridad.md).

## Mapa de funcionalidades

| Dominio | Qué resuelve | Endpoints | Documento |
|---|---|---|---|
| `auth` | Login, renovación del token y logout. | 3 | [dominios/auth.md](dominios/auth.md) |
| `user` | Registro, perfil propio, perfil público, baja de la cuenta y listado de usuarios (ADMIN). | 6 | [dominios/user.md](dominios/user.md) |
| `category` | Categorías que clasifican campañas e intereses de usuarios; el ADMIN las gestiona. | 4 | [dominios/category.md](dominios/category.md) |
| `campaign` | Campañas con meta, estados (`ACTIVE`, `COMPLETED`, `CLOSED`), historial de transiciones y visibilidad. | 9 | [dominios/campaign.md](dominios/campaign.md) |
| `donation` | Donar a una campaña, mis donaciones, donaciones de una campaña y sus estadísticas. | 4 | [dominios/donation.md](dominios/donation.md) |
| `follower` | Seguir y dejar de seguir usuarios, listas y contadores. | 7 | [dominios/follower.md](dominios/follower.md) |
| `publication` | Novedades que el dueño publica dentro de su campaña. | 3 | [dominios/publication.md](dominios/publication.md) |
| `bankaccount` | Cuentas bancarias vinculadas al usuario. | 4 | [dominios/bankaccount.md](dominios/bankaccount.md) |
| `verification` | Solicitud de verificación de cuenta y su resolución por un ADMIN. | 3 | [dominios/verification.md](dominios/verification.md) |
| `report` | Reportes de usuarios, campañas y publicaciones, y su moderación. | 5 | [dominios/report.md](dominios/report.md) |

Son 48 endpoints en total. Además, Swagger UI y la especificación OpenAPI se generan automáticamente a partir de los controllers (ver [Seguridad](03-seguridad.md)).

## Stack

Leído de `pom.xml`:

| Pieza | Versión | Para qué |
|---|---|---|
| Java | 21 | Lenguaje. |
| Spring Boot | 3.2.5 | Base de la aplicación (starters `web`, `validation`, `data-jpa`, `security`, `thymeleaf`, `devtools`). |
| Spring Security | la de Spring Boot | Autenticación stateless con JWT y autorización por rol (`@PreAuthorize`). |
| Spring Data JPA / Hibernate | la de Spring Boot | Persistencia y repositorios. |
| Microsoft SQL Server (`mssql-jdbc`) | 12.4.1 | Base de datos. |
| JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | 0.11.5 | Emisión y validación de JWT. |
| springdoc-openapi | 2.5.0 | Swagger UI y especificación OpenAPI. |
| Lombok | 1.18.30 | Getters, builders y constructores en entidades y DTOs. |
| JetBrains annotations | 26.0.2 | `@NotNull` de documentación en firmas de métodos. |
| JUnit 5, Mockito, AssertJ, `spring-security-test` | las de Spring Boot | Tests unitarios. |
| Testcontainers (`mssqlserver`) | BOM de Testcontainers | Disponible para tests de integración. |
| Maven Surefire, JaCoCo, Sonar | 3.1.2, 0.8.10, 4.0.0.4121 | Ejecución de tests en paralelo, cobertura y análisis estático. |

El proyecto se construye con el Maven wrapper (`./mvnw`), así que no hace falta Maven instalado. Cómo se levanta y se despliega está en [Infraestructura](06-infraestructura.md).

## Glosario

| Término | Significado en este proyecto |
|---|---|
| **Campaña** | Pedido de fondos de un usuario, con título, descripción, categorías y una meta. Entidad `Campaign`. |
| **Meta** | Monto objetivo (`amountGoal`), monto recaudado (`amountRaised`) y estado de una campaña. Entidad `Goal`, separada de `Campaign`; los montos son `BigDecimal` con dos decimales. |
| **Dueño** | El usuario que creó una campaña. Solo él (o un ADMIN) la modifica, la cierra o la reactiva. |
| **Estado de campaña** | `ACTIVE` (recibe donaciones), `COMPLETED` (alcanzó la meta) o `CLOSED` (cerrada por el dueño, por un ADMIN o por baja). Se guarda en `Goal.status`, y cada cambio queda en `CampaignStatusHistory`. |
| **Campaña visible** | Una campaña no borrada, cuyo dueño no está borrado ni bloqueado y que no está `CLOSED`, salvo para su dueño o un ADMIN. Lo que no es visible responde `404`, como si no existiera. |
| **Publicación** | Novedad que el dueño publica dentro de una de sus campañas. |
| **Donación** | Aporte de un usuario a una campaña. Se aprueba sola al crearse y suma al monto recaudado. |
| **Seguir** | Relación entre dos usuarios (`Follower`): uno sigue al otro. |
| **Reporte** | Denuncia de un usuario sobre otro usuario, una campaña o una publicación, con un motivo. Un ADMIN lo revisa y lo resuelve con una acción (`ActionTaken`). |
| **Sanción** | Suspensión temporal (`suspendedUntil`) o baneo (`banned`) de un usuario, aplicada al resolver un reporte. |
| **Bloqueado** | Usuario baneado o con una suspensión vigente. No puede usar la API y, para todos salvo un ADMIN, desaparece: su perfil, sus campañas y su presencia en seguidores responden `404` o se omiten. |
| **Verificación** | Solicitud de un usuario para que un ADMIN marque su cuenta como verificada. |
| **Soft delete (borrado lógico)** | Borrar es completar `deletedAt` en lugar de eliminar la fila. Lo usan `User`, `Campaign`, `Publication` y `VerificationRequest`: `@SQLDelete` convierte el `DELETE` en un `UPDATE` y `@SQLRestriction("deleted_at IS NULL")` excluye lo borrado de toda consulta. Las relaciones históricas (donaciones, reportes) siguen apuntando a la fila. |
| **Token revocado** | JWT invalidado antes de que expire (por logout o renovación), guardado en `RevokedToken`. |
