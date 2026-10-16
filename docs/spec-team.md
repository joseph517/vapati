# Resumen de `/spec-team`

**Uso:** `/spec-team NN` (o con el nombre completo del spec).

## Antes de correrlo

- El spec tiene que estar en `specs/` con **Estado: aprobado**. Si no, se detiene.
- Conviene tener el working tree limpio. Si hay cambios sin commitear, te pregunta qué hacer.
- `/spec` ya hace que el arquitecto revise el spec al guardarlo. Si lo editaste a mano después, corré `/architect check NN`: es más barato corregir el spec que el código.
- No hace falta levantar Docker: QA levanta el stack dev si lo necesita, pero nunca hace `down` ni toca nada de prod.

## Qué hace

1. Valida el spec, crea o retoma la rama `spec-NN-slug` y te muestra el resumen. Te pregunta **una sola vez** si arranca.
2. **Por cada paso, sin parar:**
   - lo implementa `java-developer`, o `doc-writer` si el paso es de documentación;
   - el revisor y QA lo verifican (tests, curl, SQL);
   - si algo falla, lo corrige (máximo 2 vueltas);
   - cuando queda validado, **hace commit**.

   En el chat ves una línea por paso.
3. **Revisión final:** el revisor y el arquitecto miran toda la rama, y el documentador actualiza `docs/arquitectura/` con su commit `docs: …`.
4. **Criterios de aceptación:** QA los verifica todos. Si todos pasan, marca `[x]`, pone **Estado:** `implementado` y mueve el spec a `specs/implementados/`.
5. **Informe final:** commits, revisión, arquitectura, criterios, docs actualizados y la recomendación sobre generar el prompt de Claude Design.

## Cuándo se detiene a preguntarte

- Hay una ambigüedad que el spec no resuelve.
- Algo sigue fallando después de 2 vueltas de corrección.
- No logra levantar el entorno.
- Un criterio no se puede verificar automáticamente.

Si tu sesión tiene notificaciones, te avisa.

## Reglas que conviene saber

- **Commits:** siguen la convención del repo (`feat: Add …`), sin mención a Claude, sin `Co-Authored-By` y sin emojis. **Nunca hace push ni merge**: eso es tuyo.
- **`specs/` está en `.gitignore`:** los `[x]`, el estado y el movimiento del spec no entran en ningún commit.
- **QA reutiliza los usuarios** de "Datos de prueba" en `specs/issues-endpoint.md` (`Passw0rd!`, `qa18adm` como ADMIN). No crea usuarios, puede borrar o banear usuarios porque es una base de prueba, y al final anota los cambios en ese registro.
- **Retomar:** si se corta la sesión, volvé a correr `/spec-team NN`. Lee `specs/.progress/NN-slug.md` y te propone desde qué paso seguir.
- **Prompt de Claude Design:** nunca lo genera solo. Te recomienda generarlo ahora, esperar o que no hace falta. Para generarlo, pedíselo o corré `/design-prompt`.

## Comandos relacionados

| Comando | Para qué |
|---|---|
| `/spec` | Escribir el spec: el arquitecto ayuda con las decisiones de arquitectura, diseña las secciones técnicas y revisa el spec al guardarlo |
| `/architect <pregunta>`, `check NN` o `evalúa <área\|todo>` | Consultas de diseño, revisar un spec, o evaluar la arquitectura y recibir recomendaciones de mejora |
| `/dev <tarea>` | Cambios chicos sin spec (no commitea salvo que se lo pidas) |
| `/document [dominio\|todo]` | Sincronizar los docs con el código |
| `/design-prompt` | Generar el prompt de Claude Design desde el último que se generó |
| `/spec-impl NN` | Implementar a mano, con pausa en cada paso |
