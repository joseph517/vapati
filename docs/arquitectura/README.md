# Documentación de arquitectura de VaPaTi

Esta carpeta explica cómo está construido el backend de VaPaTi: la arquitectura, cada funcionalidad y por qué funciona como funciona. Todo lo que dice sale del código.

**Describe:** la rama `spec-51-donacion-directa-al-creador` en el commit `6bb7c6f`, al 2026-10-16. Es una foto: los cambios posteriores al código no se reflejan acá.

**Para quién es:** un dev backend que entra al proyecto, que sabe Java y lo básico de Spring. No se explica Spring desde cero, sino cómo lo usa este proyecto.

**Convenciones:**

- Las rutas `.java` son relativas a `src/main/java/com/vaPaTi/vaPaTi/` (por ejemplo `service/campaign/CampaignService.java`). Las demás rutas se escriben desde la raíz del repo.
- Las referencias al código no llevan número de línea, para que no se desactualicen con cada cambio.
- Los diagramas están en Mermaid y se ven en GitHub y en los IDEs con vista previa de Markdown.

## Orden de lectura sugerido

1. [Visión general](01-vision-general.md): qué es VaPaTi, actores, funcionalidades, stack y glosario.
2. [Arquitectura](02-arquitectura.md): capas, paquetes y el recorrido de un request.
3. [Seguridad](03-seguridad.md): JWT, roles, sanciones y quién puede llamar a qué.
4. [Modelo de datos](04-modelo-de-datos.md): entidades, relaciones y borrado lógico.
5. [Errores y validación](05-errores-y-validacion.md): cómo se responde cuando algo falla.
6. El dominio que te toque, en [Dominios](#dominios).
7. [Infraestructura](06-infraestructura.md) y [Testing](07-testing.md), cuando tengas que levantar, desplegar o probar.
8. [Decisiones de diseño](08-decisiones-de-diseno.md): las decisiones transversales y su porqué.

## Índice

| Archivo | Contenido |
|---|---|
| [01-vision-general.md](01-vision-general.md) | Qué problema resuelve, actores, mapa de funcionalidades, stack, glosario. |
| [02-arquitectura.md](02-arquitectura.md) | Capas, organización por dominio, convenciones de nombres, recorrido de un request. |
| [03-seguridad.md](03-seguridad.md) | Autenticación JWT, refresh y logout, roles, sanciones, CORS, OpenAPI, contenedores y base de datos. |
| [04-modelo-de-datos.md](04-modelo-de-datos.md) | Entidades, relaciones, enums, borrado lógico, datos iniciales. |
| [05-errores-y-validacion.md](05-errores-y-validacion.md) | Excepciones, forma de las respuestas de error, validación de entrada, `404` vs `403`. |
| [06-infraestructura.md](06-infraestructura.md) | Perfiles, variables de entorno, Docker, hot reload, tags fijados y escaneo de vulnerabilidades, CI y publicación de imágenes. |
| [07-testing.md](07-testing.md) | Estrategia de tests, organización y cómo correrlos. |
| [08-decisiones-de-diseno.md](08-decisiones-de-diseno.md) | Decisiones transversales: qué, por qué y dónde. |
| [deuda-tecnica.md](deuda-tecnica.md) | Inconsistencias encontradas al recorrer el código. |
| [roadmap.md](roadmap.md) | Lo planeado y todavía no implementado. |
| [recomendaciones-de-arquitectura.md](recomendaciones-de-arquitectura.md) | Propuestas de mejora del architect, por prioridad. No describen el código. |

### Dominios

| Archivo | Dominio |
|---|---|
| [dominios/auth.md](dominios/auth.md) | Login, refresh y logout. |
| [dominios/user.md](dominios/user.md) | Usuarios y perfiles. |
| [dominios/category.md](dominios/category.md) | Categorías de campañas y de intereses. |
| [dominios/campaign.md](dominios/campaign.md) | Campañas, metas y estados. |
| [dominios/donation.md](dominios/donation.md) | Donaciones a campañas y directas, donaciones recibidas, estadísticas y supporters. |
| [dominios/follower.md](dominios/follower.md) | Seguir a otros usuarios. |
| [dominios/publication.md](dominios/publication.md) | Publicaciones dentro de una campaña. |
| [dominios/bankaccount.md](dominios/bankaccount.md) | Cuentas bancarias de los usuarios. |
| [dominios/verification.md](dominios/verification.md) | Solicitudes de verificación de cuenta. |
| [dominios/report.md](dominios/report.md) | Reportes de contenido y moderación. |
