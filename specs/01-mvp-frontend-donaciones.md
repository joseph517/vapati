# 01 - MVP Frontend de Donaciones (prompt para Claude Design)

**Estado:** Draft
**Depende de:** ninguno
**Fecha:** 2026-09-17

**Objetivo:** Producir un prompt completo y autocontenido, listo para pegar en Claude Design, que le permita construir en otro proyecto un MVP de frontend en Next.js para VaPaTi (login/registro, listado y detalle de campañas, creación de campaña y donación) conectado a la API real de este backend.

## Alcance

**Incluye:**
- Login y registro de usuario.
- Listado de campañas (requiere sesión) y detalle de una campaña individual.
- Creación de campaña propia.
- Flujo de donación a una campaña, reflejando el estado de auto-aprobación simulada que devuelve el backend.
- Stack: Next.js + TypeScript + Tailwind CSS + shadcn/ui, corriendo en el puerto 4200.
- Sesión simple basada en JWT (`accessToken`) guardado en el cliente y enviado en cada request autenticado.

**No incluye:**
- Publicaciones, seguidores/following, cuentas bancarias, reportes/moderación, verificación de usuario.
- Refresh token y logout con revocación de JWT (el backend los soporta, pero el MVP no los integra).
- Pasarela de pago real — el backend simula auto-aprobación de la donación, no hay integración con Stripe/PayPal ni similar.
- Perfil de usuario extendido o historial de "mis donaciones" (posible spec futura).

## Modelo de datos

No se introduce ningún modelo de datos nuevo. El frontend consume los DTOs ya existentes en el backend, documentados abajo en la sección del prompt. La única adición es un endpoint de backend (ver Plan de implementación, paso 1) que expone un DTO ya existente (`CampaignResponseDTO`) para un único recurso en vez de una lista.

## Plan de implementación

1. **Backend (este repo):** agregar `GET /api/campaigns/{campaignId}` en `CampaignController`/`CampaignService`, reutilizando `CampaignServiceValidation.findCampaignByIdOrThrow`. *(Completado como parte de este spec.)*
2. **Backend (este repo):** agregar tests unitarios para `CampaignService.getCampaignById` en `CampaignServiceTest`. *(Completado como parte de este spec.)*
3. **Entrega del prompt:** redactar el prompt final (sección siguiente) con todos los datos verificados de la API, listo para copiar y pegar en una sesión de Claude Design que construirá el frontend en otro proyecto/repo.

## Criterios de aceptación

- [ ] `GET /api/campaigns/{campaignId}` devuelve 200 y el `CampaignResponseDTO` correcto cuando la campaña existe.
- [ ] `GET /api/campaigns/{campaignId}` devuelve un error manejado (`GlobalExceptionHandler`) cuando la campaña no existe.
- [ ] `./mvnw test -Dtest=CampaignServiceTest` pasa, incluyendo los casos nuevos de `getCampaignById`.
- [ ] El archivo `specs/01-mvp-frontend-donaciones.md` existe con la sección "Prompt para Claude Design" completa y autocontenida (se puede copiar y pegar sin contexto adicional).

## Decisiones tomadas y descartadas

- **CORS:** el backend solo permite `http://localhost:4200` (`CorsConfig.java`). Se decidió **no modificar el backend** y en su lugar configurar el frontend Next.js para correr en el puerto 4200. *Descartado:* ampliar `allowedOrigins` a `:3000`, para no tocar la configuración de seguridad del backend sin necesidad.
- **Listado de campañas requiere login:** `GET /api/campaigns/list` no es una ruta pública. Se decidió **mantener el comportamiento actual** (el MVP exige login antes de navegar campañas) en vez de agregar la ruta a `PUBLIC_URLS`, para no ampliar la superficie sin autenticación sin una decisión de producto explícita.
- **Detalle de campaña:** no existía `GET /api/campaigns/{id}`. Se decidió **agregar el endpoint al backend** (en vez de que el frontend filtre el listado completo en el cliente), siguiendo el patrón ya usado por `updateCampaign`/`closeCampaign` con `CampaignServiceValidation.findCampaignByIdOrThrow`.
- **Sin pasarela de pago real:** el backend ya simula auto-aprobación de donaciones (`DonationService.createDonation`); el MVP de frontend solo refleja el estado devuelto, sin integrar un proveedor de pagos.
- **Sin refresh token en el MVP:** el backend soporta `POST /auth/refresh-token` y revocación de tokens, pero se excluye del MVP para reducir el alcance inicial del frontend.

## Riesgos

- **CORS:** si el frontend no se configura explícitamente en el puerto 4200 (o el backend cambia su configuración de CORS), todas las llamadas a la API fallarán silenciosamente por bloqueo del navegador. El prompt final lo deja como requisito explícito.
- **Rutas protegidas:** como el listado de campañas requiere JWT, cualquier intento de mostrar campañas sin sesión iniciada resultará en 401/403; el prompt aclara que el login es un prerequisito antes de cualquier pantalla de campañas.

---

## Prompt para Claude Design

> Copia y pega todo el bloque siguiente en una sesión de Claude Design (o Claude Code) en el proyecto/repo donde se construirá el frontend.

```
Quiero que construyas el MVP de un frontend en Next.js (App Router) + TypeScript para "VaPaTi", una plataforma de crowdfunding/donaciones. Debe conectarse a una API REST real que ya existe y está corriendo en http://localhost:8080 (sin prefijo de ruta / context-path).

STACK REQUERIDO
- Next.js + TypeScript
- Tailwind CSS + shadcn/ui para los componentes
- El servidor de desarrollo de Next.js debe correr en el puerto 3000 por defecto

AUTENTICACIÓN
- El backend usa JWT. Al hacer login, la respuesta trae el token en el campo `accessToken` (no "token"). Debes guardarlo en el cliente (localStorage o cookie) y enviarlo en cada request autenticado como header `Authorization: Bearer <accessToken>`.
- No implementes refresh token ni logout con revocación en este MVP — solo login simple con el accessToken.
- Todas las rutas de campañas y donaciones requieren estar autenticado. El listado de campañas NO es público: si el usuario no tiene sesión, debe ser redirigido a login antes de ver campañas.

ENDPOINTS DISPONIBLES (verificados en el código del backend)

1) POST /auth/login — público
   Request: { "email": string, "password": string }
   Response: {
     "accessToken": string,
     "refreshToken": string,
     "userInfo": {
       "userId": number,
       "email": string,
       "role": string,
       "firstName": string,
       "lastName": string,
       "userName": string,
       "fullName": string
     }
   }

2) POST /api/users/create — público (esto es el registro, NO existe /auth/register)
   Request: {
     "user": { "categoryIds": number[] },
     "userInfo": {
       "firstName": string,
       "lastName": string,
       "email": string,
       "userName": string,
       "password": string,
       "phone": string,
       "description": string,
       "profilePicture": string
     }
   }
   Response: el usuario creado (sin token). Después de registrar, hay que hacer login por separado con POST /auth/login.

3) GET /api/campaigns/list — requiere JWT
   Response: array directo (sin wrapper) de:
   {
     "id": number,
     "name": string,
     "description": string,
     "amountGoal": number,
     "amountRaised": number,
     "userId": number
   }

4) GET /api/campaigns/{campaignId} — requiere JWT
   Response: el mismo objeto CampaignResponseDTO de arriba, para una sola campaña. Si no existe, el backend responde con un error manejado (cuerpo JSON con { error, message, timestamp }).

5) POST /api/campaigns/create — requiere JWT
   Request: {
     "name": string,
     "description": string,
     "amountGoal": number,
     "amountRaised": number   // opcional, si se omite el backend lo pone en 0
   }
   Response: { "message": string, "campaignId": number, "status": "CREATED" }
   Nota: esta respuesta NO trae el objeto completo de la campaña, solo su id. Si necesitas mostrar la campaña recién creada, navega al detalle usando GET /api/campaigns/{campaignId} con el id devuelto.

6) POST /api/donations — requiere JWT
   Request: {
     "campaignId": number,
     "amount": number   // debe ser > 0
   }
   Response (HTTP 201): {
     "message": "Donation completed successfully",
     "donation": {
       "id": number,
       "donorUserId": number,
       "donorUserName": string,
       "campaignId": number,
       "campaignName": string,
       "amount": number,
       "status": "PENDING" | "COMPLETED" | "FAILED" | "REFUNDED",
       "transactionId": string,
       "createdAt": string,
       "updatedAt": string
     },
     "status": "COMPLETED"
   }
   Nota importante: el backend simula auto-aprobación — toda donación válida queda en estado COMPLETED de forma inmediata y síncrona. No hay pasarela de pago real que integrar; simplemente muestra el estado que devuelve la respuesta (ej. un mensaje de éxito con el transactionId).

PANTALLAS A CONSTRUIR
1. Login (formulario email + password, llama a POST /auth/login, guarda accessToken).
2. Registro (formulario con los campos de userInfo de arriba, llama a POST /api/users/create, y tras éxito redirige a login).
3. Listado de campañas (protegido, requiere sesión) — muestra cada campaña con nombre, descripción y progreso (amountRaised / amountGoal), consumiendo GET /api/campaigns/list.
4. Detalle de campaña (protegido) — usa GET /api/campaigns/{campaignId}, muestra la info completa y un botón/formulario para donar.
5. Crear campaña (protegido) — formulario con name, description, amountGoal, llama a POST /api/campaigns/create y redirige al detalle de la campaña creada.
6. Donar (dentro del detalle de campaña o como modal) — formulario con amount, llama a POST /api/donations, muestra confirmación con el estado COMPLETED y el transactionId.

FUERA DE ALCANCE — NO CONSTRUYAS ESTO EN EL MVP
- Publicaciones, seguir/dejar de seguir usuarios, feed social.
- Cuentas bancarias / vinculación de medios de pago propios.
- Reportes de contenido, moderación, panel de admin.
- Verificación de usuario/identidad.
- Refresh token, logout con revocación, "recordar sesión" avanzado.
- Cualquier integración con una pasarela de pago real (Stripe, PayPal, etc.) — el backend ya simula la aprobación.
- Perfil de usuario extendido o historial de "mis donaciones" (puede pedirse como iteración futura).

Estructura el proyecto de forma limpia (carpetas por feature o por ruta de Next.js según convenga), maneja estados de carga y error en cada pantalla, y usa componentes de shadcn/ui para formularios, botones, cards y alerts. Al finalizar, el flujo completo debe poder probarse manualmente: registrarse, loguearse, ver el listado de campañas, crear una campaña, ver su detalle, y donar a ella viendo la confirmación de auto-aprobación.
```
