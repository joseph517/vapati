# Resumen de `/spec-team`

**Uso:** `/spec-team NN` (o con el nombre completo del spec).

## Antes de correrlo

- El spec tiene que estar en `specs/` con **Estado: aprobado**. Si no, se detiene.
- Conviene tener el working tree limpio. Si hay cambios sin commitear, te pregunta qué hacer.
- `/spec` ya hace que el arquitecto revise el spec al guardarlo. Si lo editaste a mano después, corré `/architect check NN`: es más barato corregir el spec que el código.
- **Levantá el stack dev vos** (`docker compose -f docker-compose.dev.yml up -d`). Ningún agente arranca, para ni reconstruye la app ni la base. Si QA no la encuentra, se detiene y te avisa.

## Qué hace

1. Valida el spec, crea o retoma la rama `spec-NN-slug` y te muestra el resumen con el tipo y el nivel de cada paso. Te pregunta **una sola vez** si arranca.
2. **Por cada paso, sin parar:**
   - **paso de código:** lo implementa `java-developer` con sus tests unitarios, `spec-reviewer` revisa solo ese código y QA lo prueba con curl y SQL si el paso deja algo accesible desde afuera;
   - **paso de chequeo:** solo QA corre los curl y SQL pendientes;
   - **paso de documentación:** se deja para el pase final de docs;
   - si algo falla, lo corrige el developer (máximo 2 vueltas);
   - cuando queda validado, **hace un commit por paso**.

   En el chat ves un bloque corto por paso y nada más entre pasos.
3. **Cierre, en este orden y nunca en paralelo:**
   1. el arquitecto revisa toda la rama;
   2. se corrigen sus hallazgos de código, con review;
   3. QA corre la **suite completa una sola vez** y los criterios de aceptación que falten.
4. **Docs:** `doc-writer` actualiza `docs/arquitectura/` en una sola pasada (commit `docs: …`). Ahí también anota las recomendaciones de mejora del arquitecto en `docs/arquitectura/recomendaciones-de-arquitectura.md`.
5. **Criterios de aceptación:** si todos pasan, marca `[x]`, pone **Estado:** `implementado` y mueve el spec a `specs/implementados/`.
6. **Informe final:** commits, revisión, arquitectura, recomendaciones, criterios, docs actualizados y una línea que dice si la rama cambió la API (si la cambió, conviene correr `/design-prompt`).

## Cuándo se detiene a preguntarte

- Hay una ambigüedad que el spec no resuelve.
- Algo sigue fallando después de 2 vueltas de corrección.
- El stack dev no está levantado o no responde.
- Un criterio no se puede verificar automáticamente.

Si tu sesión tiene notificaciones, te avisa.

## Reglas que conviene saber

- **Commits:** siguen la convención del repo (`feat: Add …`), sin mención a Claude, sin `Co-Authored-By` y sin emojis. **Nunca hace push ni merge**: eso es tuyo.
- **`specs/` está en `.gitignore`:** los `[x]`, el estado y el movimiento del spec no entran en ningún commit.
- **QA reutiliza los usuarios** de "Datos de prueba" en `specs/issues-endpoint.md` (`Passw0rd!`, `qa18adm` como ADMIN). No crea usuarios, puede borrar o banear usuarios porque es una base de prueba, y al final anota los cambios en ese registro.
- **Modelos:** los agentes corren con `sonnet`/`medium` u `opus`/`medium`, según el paso; el orquestador corre con esfuerzo medio.
- **Retomar:** si se corta la sesión, volvé a correr `/spec-team NN`. Lee `specs/.progress/NN-slug.md` (se actualiza al final de cada paso) y te propone desde qué paso seguir. Un paso que quedó a medias se rehace desde el principio.
- **Prompt de Claude Design:** no es parte de `/spec-team`. Se genera aparte con `/design-prompt`, que antes te avisa si conviene esperar a un spec pendiente que vuelve a cambiar la misma API.

## Comandos relacionados

| Comando | Para qué |
|---|---|
| `/spec` | Escribir el spec: el arquitecto ayuda con las decisiones de arquitectura, diseña las secciones técnicas y revisa el spec al guardarlo |
| `/architect <pregunta>`, `check NN` o `evalúa <área\|todo>` | Consultas de diseño, revisar un spec, o evaluar la arquitectura y recibir recomendaciones de mejora |
| `/dev <tarea>` | Cambios chicos sin spec (no commitea salvo que se lo pidas) |
| `/document [dominio\|todo]` | Sincronizar los docs con el código |
| `/design-prompt` | Generar el prompt de Claude Design desde el último que se generó |
| `/spec-impl NN` | Implementar a mano, con pausa en cada paso |
