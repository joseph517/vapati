# Donation

## Para qué existe

Una donación es un aporte de dinero de un usuario autenticado a la campaña de otro usuario. Cada donación suma su monto al recaudado de la meta (`Goal`) de la campaña y, si con eso se alcanza la meta, pasa la campaña de `ACTIVE` a `COMPLETED`.

El donante puede acompañar la donación con un mensaje opcional, público o privado. Un mensaje privado lo ven solo el donante, el dueño de la campaña y un ADMIN.

El dueño de la campaña agradece de dos formas:

- **Mensaje predeterminado.** Un texto por usuario, el mismo para todas sus campañas, que el donante recibe en la respuesta de `POST /api/donations`. Se guarda en `UserInfo.autoThanksMessage` y se configura con `/api/users/me/auto-thanks` (ver [User](user.md)). No se copia en la donación.
- **Respuesta personalizada.** Una por donación, pública o privada, que el dueño escribe después. No se edita: se borra y se vuelve a escribir. La puede borrar también un ADMIN. No es un chat: el donante no contesta.

No hay pasarela de pago: el sistema **simula la aprobación**. Toda donación que pasa las validaciones se guarda directamente en `COMPLETED` con un identificador de transacción generado en el momento. El propio service lo marca como el punto donde, en producción, se integraría una pasarela.

Además de crear donaciones y responderlas, el dominio ofrece tres lecturas: el historial del usuario que llama, el listado de donaciones de una campaña y las estadísticas de recaudación de una campaña. Las dos últimas son públicas.

## Entidades y datos

### Entidad `Donation` (`entity/donation/Donation.java`, tabla `donation`)

| Campo | Tipo | Columna | Notas |
|---|---|---|---|
| `id` | `Long` | `id` | Identidad generada por la base. |
| `donor` | `User` | `donor_user_id` | `@ManyToOne` con `@NotFound(IGNORE)`, `updatable = false`. |
| `campaign` | `Campaign` | `campaign_id` | `@ManyToOne` con `@NotFound(IGNORE)`, `updatable = false`. |
| `campaignId` | `Long` | `campaign_id` | Copia de solo lectura de la FK (`insertable = false, updatable = false`). |
| `donorUserId` | `Long` | `donor_user_id` | Copia de solo lectura de la FK. |
| `amount` | `BigDecimal` | `amount` | `precision = 15, scale = 2`, no nulo. |
| `status` | `DonationStatus` | `status` | Guardado como texto (`EnumType.STRING`), por defecto `PENDING`. |
| `transactionId` | `String` | `transaction_id` | Único, hasta 100 caracteres. Formato `TXN-XXXXXXXX`. |
| `message` | `String` | `message` | Mensaje opcional del donante, ya sin espacios en los extremos. `varchar(280)`, nulo. `null` si la donación no tiene mensaje. |
| `messagePrivate` | `boolean` | `message_private` | `true` si el mensaje es privado. `bit NOT NULL` con `@ColumnDefault("0")` y `@Builder.Default` en `false`. Nunca es `true` sin mensaje. |
| `thanksMessage` | `String` | `thanks_message` | Respuesta del dueño de la campaña, ya sin espacios en los extremos. `varchar(280)`, nulo. `null` si no hay respuesta. |
| `thanksPrivate` | `boolean` | `thanks_private` | `true` si la respuesta es privada. `bit NOT NULL` con `@ColumnDefault("0")` y `@Builder.Default` en `false`. Es independiente de `messagePrivate`. |
| `thankedAt` | `LocalDateTime` | `thanked_at` | Cuándo se escribió la respuesta. `datetime2(6)`, nulo. |
| `createdAt` / `updatedAt` | `LocalDateTime` | `created_at` / `updated_at` | Marcas de tiempo de Hibernate más `@PrePersist`/`@PreUpdate`. |

**Por qué cada FK está mapeada dos veces.** Las campañas y los usuarios tienen borrado lógico (`@SQLRestriction("deleted_at IS NULL")`). Si la relación fuera obligatoria, Hibernate haría un inner join contra la tabla filtrada y la donación desaparecería al borrar su campaña o su donante. Por eso:

- `donor` y `campaign` son opcionales en el mapeo (la columna sigue siendo `NOT NULL` en la base) y llevan `@NotFound(action = NotFoundAction.IGNORE)`: si el destino está borrado, la relación queda en `null` en lugar de perder la fila.
- Son `updatable = false`, así un `UPDATE` de la donación nunca escribe ese `null` sobre la FK.
- `campaignId` y `donorUserId` son copias de solo lectura de las mismas columnas: conservan el valor aunque la relación sea `null` y permiten filtrar por FK sin hacer join contra la tabla con borrado lógico (las consultas de estadísticas y el historial filtran por estas columnas).

El detalle general del borrado lógico está en [Modelo de datos](../04-modelo-de-datos.md).

**Por qué `message_private` y `thanks_private` llevan default en la base.** Las columnas las crea `ddl-auto=update`. Sin `@ColumnDefault("0")`, el `ALTER TABLE donation ADD ... bit NOT NULL` falla en SQL Server si la tabla ya tiene filas. Con el default, las donaciones anteriores al mensaje quedaron con `message = NULL` y `message_private = 0`, y las anteriores a la respuesta con `thanks_message = NULL`, `thanks_private = 0` y `thanked_at = NULL`.

**Invariante de la respuesta.** `thanksMessage == null` si y solo si `thankedAt == null`, y sin respuesta `thanksPrivate` es `false`. Responder y borrar escriben los tres campos juntos. La respuesta es la única parte de una donación que cambia después de crearla.

**`message` y `thanks_message` son `varchar`, no `nvarchar`.** Siguen la convención del resto de los textos del proyecto, como `publication.description`. Las tildes, la `ñ` y los signos `¡` y `¿` se guardan bien. Los emojis y los caracteres fuera de la página de códigos de SQL Server llegan como `?`. Lo mismo vale para `user_info.auto_thanks_message`.

### Enum `DonationStatus` (`entity/donation/DonationStatus.java`)

`PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`. Hoy el código solo asigna `COMPLETED` (ver [Estados](#estados)).

### Datos de otros dominios que se usan

- `Campaign` y `Goal` (`entity/campaign/`): la donación apunta a la campaña, y el monto se suma en `Goal.amountRaised`. `Goal.amountRaised` es `updatable = false` en el mapeo: solo lo modifica `GoalRepository.addToAmountRaised` con un `UPDATE` atómico, para que guardar una campaña con datos viejos nunca pise donaciones sumadas en el medio. Ver [Campaign](campaign.md).
- `CampaignStatus` (`entity/campaign/CampaignStatus.java`): el estado de la meta (`ACTIVE`, `COMPLETED`, `CLOSED`) decide si se puede donar.
- `CampaignStatusHistory`: la donación que completa la meta registra la transición `ACTIVE → COMPLETED` con `changedBy = null` (la hizo el sistema, no un usuario).
- `User` (`entity/user/User.java`): el donante. Su nombre visible sale de `userInfo.userName`.
- `UserInfo.autoThanksMessage` (`entity/user/UserInfo.java`): el mensaje predeterminado del dueño de la campaña, que `createDonation` lee de `campaign.getUser().getUserInfo()`. Lo administra el dominio [User](user.md).

### DTOs

- `dtos/donation/CreateDonationDTO.java`: entrada de `POST /api/donations`.
- `dtos/donation/DonationCreatedResponseDTO.java`: lo que devuelve `DonationService.createDonation`, la donación (`donation`) más el mensaje predeterminado del dueño (`autoThanksMessage`). El controller lo reparte en el cuerpo de la respuesta.
- `dtos/donation/CreateDonationThanksDTO.java`: entrada de `POST /api/donations/{donationId}/thanks`.
- `dtos/donation/DonationResponseDTO.java`: una donación en las respuestas.
- `dtos/donation/CampaignStatisticsDTO.java`: estadísticas de una campaña. Vive en `dtos/donation/` aunque describe una campaña, porque lo calcula este dominio a partir de las donaciones.

## Reglas de negocio

Todas las aplica `validation/donation/DonationValidationService.java`, salvo donde se indica otra clase. El orden es el de `DonationService.createDonation`.

1. **El donante sale del token.** `AuthenticatedUserService.getAuthenticatedUserId()`; el body no lleva id de usuario. Así nadie puede donar en nombre de otro.
2. **Entrada válida.** `campaignId` y `amount` obligatorios, `amount > 0`, con hasta 13 enteros y 2 decimales. Lo valida Bean Validation en `CreateDonationDTO` (`@NotNull`, `@Positive`, `@Digits`) y lo repite `validateInput` con los mismos mensajes. El límite de dígitos coincide con la columna (`precision = 15, scale = 2`), así que un monto aceptado siempre entra en la base sin redondeo.
3. **Mensaje opcional** (`validateAndNormalizeMessage`, que delega el texto en `validation/common/TextValidationService.normalizeRequiredText`). Sin `message`, o con `message: null`, la donación no tiene mensaje. Con texto, se guarda con `trim`. `""` o solo espacios (incluidos saltos de línea y tabulaciones) responden `400 Message cannot be blank`: el mensaje es opcional, pero si llega tiene que tener texto. El largo máximo es 280 caracteres (`MAX_MESSAGE_LENGTH`): lo corta `@Size(max = 280)` en `CreateDonationDTO`, y `validateAndNormalizeMessage` lo repite sobre el texto ya recortado. `messagePrivate` ausente o `null` cuenta como `false`, y `messagePrivate: true` sin mensaje se guarda como `false`, porque no hay nada que ocultar.
4. **La campaña tiene que ser visible para el donante.** `validateAndGetCampaign` delega en `CampaignValidationService.findVisibleCampaignByIdOrThrow`: una campaña inexistente, borrada, `CLOSED` de otro usuario, de un dueño borrado o de un dueño baneado o suspendido responde lo mismo, `404 Campaign not found with id: {id}`. Así, donar no sirve para descubrir campañas que el usuario no puede ver. Un ADMIN ve toda campaña de un dueño no borrado (también las `CLOSED` y las de dueños sancionados). Como la regla es la misma que para leer, un ADMIN puede donar a la campaña de un dueño baneado o suspendido; a una `CLOSED` no, porque la corta la regla siguiente. El criterio completo de visibilidad está en [Campaign](campaign.md).
5. **La meta tiene que existir y no estar `CLOSED`** (`validateGoalIsActive`). Una campaña `COMPLETED` **sí** acepta donaciones: la meta alcanzada no corta la recaudación, y el recaudado puede superar la meta. Solo el cierre la corta. El caso "sin meta" (`Campaign does not have a goal`) no aparece con datos creados por la API: `CampaignMapper` crea cada campaña con su `Goal` en `0` recaudado.
6. **El donante existe** (`validateAndGetDonor`, `UserRepository.findById`). En la práctica siempre se cumple, porque el filtro JWT ya cargó al usuario.
7. **No se puede donar a la campaña propia** (`validateNotSelfDonation`), comparando el id del token con el dueño de la campaña. Evita que un usuario infle su propia recaudación.
8. **Aprobación simulada.** Se construye la `Donation` con `status = COMPLETED` y un `transactionId` `"TXN-"` + los primeros 8 caracteres de un UUID aleatorio en mayúsculas, más el mensaje normalizado y su `messagePrivate`, y se guarda.
9. **Suma atómica del recaudado** (`GoalRepository.addToAmountRaised`): un `UPDATE goal SET amount_raised = amount_raised + :amount ... WHERE id = :goalId AND status <> CLOSED`. La suma la hace la base, así dos donaciones simultáneas no se pisan (no hay lectura-modificación-escritura en Java). Si devuelve `0` filas, la campaña se cerró entre la validación y la suma: se lanza `400 Campaign goal is not active` y, como el método es `@Transactional`, se revierte también el `INSERT` de la donación.
10. **Paso automático a `COMPLETED`** (`GoalRepository.completeIfGoalReached`): un segundo `UPDATE` que solo cambia la meta si sigue `ACTIVE` y `amount_raised >= amount_goal`. Devuelve `1` únicamente para la donación que produjo el cambio, así la transición se registra una sola vez en el historial aunque varias donaciones crucen la meta a la vez.
11. **Mensaje predeterminado del dueño.** Lee el `autoThanksMessage` vigente del dueño de la campaña, o `null` si no configuró ninguno (o si el dueño o su `userInfo` no están). Viaja solo en la respuesta: no se guarda en la donación, así que cambiarlo o borrarlo después no afecta a las donaciones ya hechas, y no aparece en ninguna otra lectura.

### Responder a una donación

Las aplica `DonationValidationService`, en el orden de `DonationService.thankDonation` y `deleteDonationThanks`.

1. **Quien llama sale del token**, como al donar.
2. **Texto obligatorio** (`validateAndNormalizeThanksMessage`, solo al responder). Ausente, `null`, `""` o solo espacios → `400 Thanks message cannot be blank`. Se guarda con `trim`. El largo máximo es 280: lo corta `@Size(max = 280)` en `CreateDonationThanksDTO` (`400 Validation failed`) y la validación lo repite sobre el texto recortado, igual que el mensaje del donante. Se valida antes de cargar la donación, así que un texto en blanco responde `400` aunque la donación no exista.
3. **La donación existe** (`validateAndGetDonation`) → si no, `404 Donation not found with id: {id}`.
4. **La campaña no está borrada** (`validateAndGetDonationCampaign`). Una campaña con soft delete deja la relación `campaign` en `null` (`@NotFound`): en sus donaciones no se puede responder ni borrar la respuesta, `400 Cannot reply to a donation of a deleted campaign`, tampoco un ADMIN. El estado del donante (borrado o sancionado) no importa, y una campaña `CLOSED` o `COMPLETED` sí acepta respuestas: la única restricción de campaña es el borrado.
5. **Quién puede.**
   - Responder: solo el dueño de la campaña (`validateIsCampaignOwner`, `403 Only the campaign owner can reply to this donation`). Un ADMIN **no**: el chequeo no consulta el rol. El ADMIN modera borrando.
   - Borrar: el dueño o un ADMIN (`validateIsCampaignOwnerOrAdmin`, `403 Only the campaign owner or an admin can delete this reply`). El rol se lee de la base con `CampaignAuthorizationService.isAdmin`, y solo si quien llama no es el dueño.
6. **Solo donaciones `COMPLETED`** (`validateDonationCompleted`, solo al responder) → `400 Only completed donations can be replied to`. Hoy toda donación es `COMPLETED`, así que no se alcanza por la API.
7. **Una respuesta por donación.**
   - Responder con una respuesta ya escrita → `409 Donation already has a thanks message` (`validateHasNoThanks`). No se edita.
   - Borrar sin respuesta → `404 Donation has no thanks message` (`validateHasThanks`).
8. **Privacidad propia.** `thanksPrivate` ausente o `null` cuenta como `false`. Es independiente de `messagePrivate`: se puede responder en público a un mensaje privado, y al revés. Se puede responder a cualquier donación, tenga o no mensaje del donante.
9. **Guardado.** Responder fija `thanksMessage`, `thanksPrivate` y `thankedAt = now`. Borrar pone `null`, `false` y `null`. Después de borrar, el dueño puede responder de nuevo.

**Concurrencia.** El chequeo de `409` es una lectura seguida de una escritura, sin lock ni `UPDATE` condicional: dos respuestas simultáneas del dueño a la misma donación pasan las dos el chequeo y gana la última. Como solo escribe el dueño, el caso es un doble clic, y el frontend deshabilita el botón al enviar.

**Concurrencia.** No hay locks explícitos en este dominio: la protección son los dos `UPDATE` condicionales. El primero bloquea la fila de la meta hasta el commit, que llega enseguida porque son las últimas escrituras de la transacción. Del otro lado, las operaciones de campaña que deciden un estado (cerrar, reabrir, editar el monto de la meta, borrar, cerrar todas al borrar la cuenta) releen la meta con `PESSIMISTIC_WRITE` (`CampaignService.lockGoal`), así que nunca se intercalan con una suma a medio hacer. Borrar una campaña primero la pasa a `CLOSED`, por lo que una donación en curso contra una campaña que se está borrando también cae en el caso de 0 filas y se revierte. La meta cargada en memoria queda desactualizada después de los `UPDATE` y el service no la vuelve a usar.

**Montos.** Todo es `BigDecimal`. Las estadísticas redondean a 2 decimales con `RoundingMode.HALF_UP`.

**Identificador de transacción.** Los 8 caracteres son hexadecimales (32 bits de un UUID). La columna es única, así que si sale un valor repetido el `INSERT` falla y la donación responde `409` sin reintentar.

**Validación duplicada y código inalcanzable.** `validateInput` repite lo que ya cortó Bean Validation antes de entrar al controller. `validateAndGetCampaign` también controla `campaign.getDeletedAt() != null` (`400 Cannot donate to a deleted campaign`), pero una campaña borrada nunca llega ahí: `@SQLRestriction` la excluye de la consulta y la respuesta es el `404`. El mismo método lo usan los dos endpoints de lectura, no solo el de donar. El chequeo de largo de `validateAndNormalizeMessage` tampoco se alcanza por la API: `@Size` mide el texto con los espacios de los extremos, así que todo lo que pasa el DTO entra en 280 después del `trim`. Al revés, `@Size` sí puede rechazar un texto que recortado entraría (278 caracteres con 3 espacios alrededor). Es un caso raro, y el cliente debería mandar el texto ya recortado.

El mensaje en blanco se rechaza en la validación (`400 Business logic error`) y no con un `@Pattern` en el DTO, porque las reglas de negocio viven en `validation/`. El largo se corta en el DTO (`400 Validation failed`), igual que el monto.

## Estados

`DonationStatus` declara cuatro valores, pero con la aprobación simulada solo uno se alcanza:

```mermaid
stateDiagram-v2
    [*] --> COMPLETED : POST /api/donations (aprobación simulada)
    COMPLETED --> [*]
    PENDING : PENDING - valor por defecto de la entidad, nunca persistido
    FAILED : FAILED - declarado, sin uso
    REFUNDED : REFUNDED - declarado, sin uso
```

- `PENDING` es el valor por defecto del builder y de `@PrePersist`, pero `createDonation` siempre asigna `COMPLETED` antes de guardar.
- `FAILED` y `REFUNDED` no se asignan en ningún lugar. No hay endpoint para cambiar el estado de una donación ni para reembolsarla: el estado, el monto y el mensaje del donante no cambian una vez creada. Lo único que cambia es la respuesta del dueño (`thanksMessage`, `thanksPrivate`, `thankedAt`).
- Las estadísticas cuentan solo donaciones `COMPLETED`; los listados devuelven todas sin filtrar por estado (hoy es lo mismo).

La donación además dispara una transición de **otra** máquina de estados: la meta de la campaña pasa de `ACTIVE` a `COMPLETED` cuando el recaudado alcanza la meta. Ese diagrama está en [Campaign](campaign.md).

```mermaid
sequenceDiagram
    participant C as Cliente
    participant S as DonationService
    participant V as DonationValidationService
    participant DB as Base de datos
    C->>S: POST /api/donations
    S->>V: validateInput, validateAndNormalizeMessage
    S->>V: validateAndGetCampaign, validateGoalIsActive
    S->>V: validateAndGetDonor, validateNotSelfDonation
    S->>DB: INSERT donation (COMPLETED, TXN-..., mensaje)
    S->>DB: addToAmountRaised (suma si la meta no está CLOSED)
    alt 0 filas (campaña cerrada en el medio)
        S-->>C: 400 Campaign goal is not active (rollback)
    else 1 fila
        S->>DB: completeIfGoalReached (COMPLETED si sigue ACTIVE y alcanzó la meta)
        opt 1 fila
            S->>DB: INSERT campaign_status_history (ACTIVE a COMPLETED)
        end
        S->>DB: lee user_info.auto_thanks_message del dueño
        S-->>C: 201 Donation completed successfully (con autoThanksMessage)
    end
```

La respuesta del dueño no tiene un enum: su estado se deduce de `thanksMessage`.

```mermaid
stateDiagram-v2
    [*] --> SinRespuesta : donación creada
    SinRespuesta --> ConRespuesta : POST /thanks (dueño)
    ConRespuesta --> ConRespuesta : POST /thanks → 409
    ConRespuesta --> SinRespuesta : DELETE /thanks (dueño o ADMIN)
    SinRespuesta --> SinRespuesta : DELETE /thanks → 404
    SinRespuesta : Sin respuesta - thanksMessage null
    ConRespuesta : Con respuesta - texto, thanksPrivate, thankedAt
```

## Visibilidad y permisos

- **Crear:** cualquier usuario autenticado (USER o ADMIN), no sancionado, sobre una campaña que pueda ver y que no sea suya.
- **Responder y borrar la respuesta:** ver [Responder a una donación](#responder-a-una-donación). Solo el dueño responde; el dueño o un ADMIN borran.
- **Historial propio** (`/my-donations`): solo las donaciones del que llama, con el mensaje completo de cada una y la respuesta del dueño, también los privados. Incluye donaciones a campañas que después se cerraron, se borraron o cuyo dueño fue sancionado o borrado: la consulta filtra solo por el donante y no aplica la visibilidad de la campaña. En consecuencia, el historial puede mostrar el id y el nombre de una campaña que, pedida por `/api/campaigns/{campaignId}`, responde `404`; solo la borrada se distingue, con `campaignDeleted = true`. Una campaña borrada aparece con `campaignDeleted = true`, su id y su nombre, que se recupera con una consulta nativa (`CampaignRepository.findNamesByIdsIncludingDeleted`) que saltea el borrado lógico.
- **Donaciones y estadísticas de una campaña:** públicas (también sin token), pero con el mismo criterio de visibilidad de la campaña que al donar. Una campaña que el que llama no puede ver responde `404 Campaign not found with id: {id}`, sea porque no existe, está borrada, es `CLOSED` ajena o su dueño está sancionado.
- **Quién es el donante es público.** El listado de una campaña muestra `donorUserId` y `donorUserName` a cualquiera. Un donante con la cuenta borrada aparece anónimo (`donorUserId` y `donorUserName` en `null`), porque la relación queda en `null`. El listado no filtra donantes baneados o suspendidos: aparecen con su id y su nombre, para cualquiera (a diferencia de los perfiles y los listados de seguidores, que los ocultan a quien no es ADMIN). Las estadísticas también los cuentan.
- **El `transactionId` no es público.** Es la referencia de pago. En `GET /api/donations/campaign/{campaignId}`:
  - el dueño de la campaña y los ADMIN ven todos;
  - un donante ve solo los de sus propias donaciones;
  - cualquier otro, incluido el anónimo, no ve ninguno (`transactionId = null`).
- **Un mensaje privado no es público.** Sigue la misma regla que el `transactionId`, y la decide el mismo chequeo (`DonationService.canSeePrivateDonationData`). En `GET /api/donations/campaign/{campaignId}`, una donación con mensaje privado se ve así:

  | Quién llama | `message` | `messagePrivate` |
  |---|---|---|
  | Dueño de la campaña | El texto | `true` |
  | ADMIN | El texto | `true` |
  | El donante de esa donación | El texto | `true` |
  | Otro USER o un anónimo | `null` | `false` |

  Para quien no puede leerlo, la donación se ve como una sin mensaje: `messagePrivate: true` con `message: null` revelaría que hay un mensaje oculto. Un mensaje público lo ve cualquiera, con `messagePrivate: false`. La privacidad es solo del mensaje: el donante sigue siendo público. `POST /api/donations` y `/my-donations` devuelven siempre el mensaje completo, porque solo los ve el donante. Toda ruta nueva que exponga donaciones tiene que ocultar el mensaje privado con `canSeePrivateDonationData`. El mapper no decide la visibilidad, porque no conoce a quien llama.
- **Una respuesta privada no es pública.** Mismo criterio y mismo chequeo que el mensaje privado, y es independiente de él. En `GET /api/donations/campaign/{campaignId}`, una donación con respuesta privada se ve así:

  | Quién llama | `thanksMessage` | `thanksPrivate` | `thankedAt` |
  |---|---|---|---|
  | Dueño de la campaña | El texto | `true` | La fecha |
  | ADMIN | El texto | `true` | La fecha |
  | El donante de esa donación | El texto | `true` | La fecha |
  | Otro USER o un anónimo | `null` | `false` | `null` |

  Para el resto la donación se ve como una sin respuesta, también sin `thankedAt`: una fecha con el texto vacío revelaría que hay algo oculto. Una respuesta pública la ve cualquiera. `/my-donations` y las respuestas de `POST` y `DELETE /thanks` la devuelven completa, porque solo las piden el donante, el dueño o un ADMIN. Toda ruta nueva que exponga donaciones (por ejemplo un feed de apoyos o una lista de supporters) tiene que ocultarla con `canSeePrivateDonationData`, igual que el mensaje.
- **ADMIN.** Se decide con `CampaignAuthorizationService.isAdmin` (consulta `existsByIdAndRole_Name(id, "ADMIN")` contra la base, no el rol del token), tanto para la visibilidad de la campaña como para ver todos los `transactionId` y los mensajes privados. Un ADMIN puede donar como cualquier usuario, también a campañas de dueños sancionados (ver [Reglas de negocio](#reglas-de-negocio)).
- **404 vs 403.** Al donar y en las lecturas, toda campaña que no se puede ver da `404`, con el mismo mensaje que una inexistente. Los dos endpoints de `/thanks` son la excepción: responden `403` a quien no es el dueño (o ADMIN, al borrar) sin aplicar la visibilidad de la campaña. Un tercero que prueba ids recibe `404` si la donación no existe y `403` si existe, también cuando es de una campaña `CLOSED` ajena o de un dueño sancionado, que en el resto de la API darían `404`. El `400` de campaña borrada también confirma que la donación existe. Además está el `403` del filtro JWT para cuentas baneadas o suspendidas (ver [Seguridad](../03-seguridad.md)), que corta también las rutas públicas si el usuario sancionado manda su token.

## Endpoints

Las respuestas de error tienen la forma `{error, message, timestamp}` descripta en [Errores y validación](../05-errores-y-validacion.md); las de Spring Security (`401`/`403` del filtro) agregan `path`.

### `POST /api/donations`

- **Acceso:** usuario autenticado, USER o ADMIN (`anyRequest().authenticated()` en `SecurityConfig`; no hay `@PreAuthorize`). Sin token, o con un token inválido, expirado, revocado, de refresh, emitido antes del último cambio de credenciales o de una cuenta borrada → `401`. Cuenta baneada o suspendida → `403` desde el filtro.
- **Parámetros:** ninguno.
- **Body:** `CreateDonationDTO`, JSON.

  | Campo | Tipo | Obligatorio | Validación |
  |---|---|---|---|
  | `campaignId` | `Long` | sí | `@NotNull` |
  | `amount` | `BigDecimal` | sí | `@NotNull`, `@Positive`, `@Digits(integer = 13, fraction = 2)` |
  | `message` | `String` | no | `@Size(max = 280, message = "Message must be at most 280 characters")`. Si llega, no puede estar en blanco (regla de negocio). |
  | `messagePrivate` | `Boolean` | no | Ausente o `null` cuenta como `false`. Sin `message`, se guarda `false`. |

- **Respuesta:** `201 Created`.

  ```json
  {
    "message": "Donation completed successfully",
    "donation": { "...": "DonationResponseDTO" },
    "status": "COMPLETED",
    "autoThanksMessage": "¡Gracias por tu apoyo!"
  }
  ```

  `autoThanksMessage` es el mensaje predeterminado vigente del dueño de la campaña, o `null` si no configuró ninguno. La clave siempre está. El frontend lo muestra como confirmación al donante. El cuerpo se arma con un `LinkedHashMap`, porque `Map.of` no acepta valores `null`.

  `DonationResponseDTO`:

  | Campo | Tipo | Descripción |
  |---|---|---|
  | `id` | `Long` | Id de la donación. |
  | `donorUserId` | `Long` | Id del donante; `null` si su cuenta está borrada. |
  | `donorUserName` | `String` | Nombre de usuario del donante; `null` si su cuenta está borrada. |
  | `campaignId` | `Long` | Id de la campaña (se conserva aunque esté borrada). |
  | `campaignName` | `String` | Nombre de la campaña. |
  | `campaignDeleted` | `Boolean` | `true` si la campaña está borrada. |
  | `amount` | `BigDecimal` | Monto donado. |
  | `status` | `DonationStatus` | Siempre `COMPLETED` hoy. |
  | `transactionId` | `String` | `TXN-` + 8 caracteres hexadecimales en mayúsculas. |
  | `message` | `String` | Mensaje del donante, sin espacios en los extremos; `null` si no tiene, o si es privado y el que llama no puede verlo. |
  | `messagePrivate` | `Boolean` | `true` si el mensaje es privado; `false` si es público, si no hay mensaje, o si es privado y el que llama no puede verlo. |
  | `thanksMessage` | `String` | Respuesta del dueño, sin espacios en los extremos; `null` si no hay, o si es privada y el que llama no puede verla. Al crear la donación siempre es `null`. |
  | `thanksPrivate` | `Boolean` | `true` si la respuesta es privada; `false` si es pública, si no hay respuesta, o si es privada y el que llama no puede verla. |
  | `thankedAt` | `LocalDateTime` | Cuándo se respondió; `null` si no hay respuesta, o si es privada y el que llama no puede verla. |
  | `createdAt` / `updatedAt` | `LocalDateTime` | Marcas de tiempo. |

  El `status` del primer nivel de la respuesta es un literal del controller, no se lee de la donación.

- **Errores:**

  | Status | Mensaje | Cuándo |
  |---|---|---|
  | 401 | `Full authentication is required to access this resource` | Sin token o con un token que el filtro rechaza (ver Acceso). |
  | 403 | `Your account has been banned. Reason: {motivo}` | El que llama está baneado (filtro JWT). |
  | 403 | `Your account is suspended until {fecha}. Reason: {motivo}` | El que llama tiene una suspensión vigente (filtro JWT). |
  | 400 | `One or more fields are invalid`, con `fields.campaignId = "Campaign ID is required"` | Falta `campaignId`. |
  | 400 | `One or more fields are invalid`, con `fields.amount = "Amount is required"` | Falta `amount`. |
  | 400 | `One or more fields are invalid`, con `fields.amount = "Amount must be greater than zero"` | `amount` es 0 o negativo. |
  | 400 | `One or more fields are invalid`, con `fields.amount = "numeric value out of bounds (<13 digits>.<2 digits> expected)"` (mensaje por defecto de Hibernate Validator, el DTO no define uno propio) | Más de 13 enteros o más de 2 decimales. |
  | 400 | `One or more fields are invalid`, con `fields.message = "Message must be at most 280 characters"` | `message` de más de 280 caracteres, contando los espacios de los extremos. |
  | 400 | `Request body is required` | Body ausente. |
  | 400 | `Invalid value '{valor}' for field '{campo}'` | Un campo con un valor que no se puede convertir (por ejemplo `"amount": "abc"`). |
  | 400 | `Malformed request body` | JSON mal formado. |
  | 415 | `Content type '{tipo}' is not supported. Use application/json` | `Content-Type` distinto de JSON. |
  | 400 | `Message cannot be blank` (con `error: "Business logic error"`) | `message` es `""` o solo espacios. No se guarda la donación ni se suma al recaudado. |
  | 404 | `Campaign not found with id: {id}` | La campaña no existe, está borrada, es `CLOSED` de otro usuario, o su dueño está borrado, baneado o suspendido. Para un ADMIN, solo si no existe, está borrada o su dueño está borrado. |
  | 400 | `Campaign does not have a goal` | La campaña no tiene meta. Solo con datos cargados por fuera de la API: toda campaña nace con meta. |
  | 400 | `Campaign goal is not active` | La meta está `CLOSED` (campaña propia cerrada, o cualquier campaña cerrada si llama un ADMIN), o se cerró entre la validación y la suma; en ese caso la donación se revierte. |
  | 404 | `Donor user not found with id: {id}` | El usuario del token no existe. No ocurre en la práctica: el filtro ya lo cargó. |
  | 400 | `Cannot donate to your own campaign` | El que llama es el dueño de la campaña. |
  | 409 | `The request conflicts with existing data` | Violación de una restricción de la base al guardar, por ejemplo un `transactionId` repetido. |

  Los errores de Bean Validation se juntan en `fields`, uno por campo: si `amount` rompe varias reglas a la vez (por ejemplo `-0.001`), se ve solo uno de los mensajes. Las reglas de negocio corren en el orden de la tabla y responde la primera que falla: el dueño que dona a su propia campaña `CLOSED` recibe `Campaign goal is not active`, no `Cannot donate to your own campaign`.

- **Qué hace:**
  1. Obtiene el id del donante del token (`AuthenticatedUserService.getAuthenticatedUserId`).
  2. `validateInput(dto)`.
  3. `validateAndNormalizeMessage(dto.message)`: el mensaje con `trim`, `null` si no hay, o `400`. `messagePrivate` queda en `true` solo si hay mensaje y el request trae `messagePrivate: true`.
  4. `validateAndGetCampaign(campaignId, donorId)`: carga la campaña visible para el donante o `404`.
  5. `validateGoalIsActive(campaign.getGoal())`.
  6. `validateAndGetDonor(donorId)`.
  7. `validateNotSelfDonation(donorId, ownerId)`.
  8. Construye la donación en `COMPLETED` con un `transactionId` nuevo, el mensaje y `messagePrivate`, y la guarda.
  9. `GoalRepository.addToAmountRaised`; si actualiza 0 filas, lanza `400 Campaign goal is not active` y la transacción se revierte.
  10. `GoalRepository.completeIfGoalReached`; si actualiza 1 fila, `CampaignStatusHistoryService.recordTransition(campaign, ACTIVE, COMPLETED, null)`.
  11. Mapea con `DonationMapper.toDTO`, lee el `autoThanksMessage` del dueño y devuelve un `DonationCreatedResponseDTO`. El controller arma la respuesta con un `LinkedHashMap`. El mensaje vuelve completo, aunque sea privado.

### `POST /api/donations/{donationId}/thanks`

- **Acceso:** usuario autenticado (`anyRequest().authenticated()`, sin `@PreAuthorize`). Solo tiene éxito el dueño de la campaña: un ADMIN recibe `403`. Sin token o con token rechazado → `401`; cuenta sancionada → `403` del filtro.
- **Parámetros:**

  | Nombre | Ubicación | Tipo | Obligatorio |
  |---|---|---|---|
  | `donationId` | path | `Long` | sí |

- **Body:** `CreateDonationThanksDTO`, JSON.

  | Campo | Tipo | Obligatorio | Validación |
  |---|---|---|---|
  | `thanksMessage` | `String` | sí | `@Size(max = 280, message = "Thanks message must be at most 280 characters")`. Ausente o en blanco → `400` (regla de negocio). |
  | `thanksPrivate` | `Boolean` | no | Ausente o `null` cuenta como `false`. |

- **Respuesta:** `200 OK`.

  ```json
  {
    "message": "Thanks sent successfully",
    "donation": { "...": "DonationResponseDTO" }
  }
  ```

  La donación vuelve completa, con `transactionId`, el mensaje del donante y la respuesta recién guardada. El `updatedAt` de la respuesta es el anterior: el DTO se arma antes del flush que dispara `@UpdateTimestamp`, aunque en la base queda actualizado.

- **Errores:**

  | Status | Mensaje | Cuándo |
  |---|---|---|
  | 401 | `Full authentication is required to access this resource` | Sin token o con un token que el filtro rechaza. |
  | 403 | `Your account has been banned. Reason: {motivo}` / `Your account is suspended until {fecha}. Reason: {motivo}` | El que llama está sancionado (filtro JWT). |
  | 400 | `Invalid value '{valor}' for parameter 'donationId'` | `donationId` no es un número. |
  | 400 | `Request body is required` / `Malformed request body` | Sin body o JSON inválido. |
  | 400 | `One or more fields are invalid`, con `fields.thanksMessage = "Thanks message must be at most 280 characters"` | Más de 280 caracteres, contando los espacios de los extremos. |
  | 400 | `Thanks message cannot be blank` (con `error: "Business logic error"`) | `thanksMessage` ausente, `null`, `""` o solo espacios. |
  | 404 | `Donation not found with id: {id}` | La donación no existe. |
  | 400 | `Cannot reply to a donation of a deleted campaign` | La campaña está borrada. |
  | 403 | `Only the campaign owner can reply to this donation` | El que llama no es el dueño, incluido un ADMIN. |
  | 400 | `Only completed donations can be replied to` | La donación no es `COMPLETED`. No ocurre hoy. |
  | 409 | `Donation already has a thanks message` | La donación ya tiene respuesta. El texto no cambia. |

  Responde la primera regla que falla, en el orden de la tabla.

- **Qué hace** (`DonationService.thankDonation`, `@Transactional`):
  1. Obtiene el id del que llama del token.
  2. `validateAndNormalizeThanksMessage(dto.thanksMessage)`.
  3. `validateAndGetDonation` → `validateAndGetDonationCampaign` → `validateIsCampaignOwner` → `validateDonationCompleted` → `validateHasNoThanks`.
  4. Fija `thanksMessage`, `thanksPrivate` (`Boolean.TRUE.equals(dto.thanksPrivate)`) y `thankedAt = now`, guarda y mapea con `DonationMapper.toDTO`.

### `DELETE /api/donations/{donationId}/thanks`

- **Acceso:** usuario autenticado. Tienen éxito el dueño de la campaña y un ADMIN. Sin token o con token rechazado → `401`; cuenta sancionada → `403` del filtro.
- **Parámetros:** `donationId` (path, `Long`, obligatorio).
- **Body:** sin body.
- **Respuesta:** `200 OK`, con `{message: "Thanks deleted successfully", donation}`. La donación vuelve con `thanksMessage: null`, `thanksPrivate: false` y `thankedAt: null`, y con el mismo `updatedAt` desactualizado que al responder. Después, el dueño puede responder otra vez.
- **Errores:**

  | Status | Mensaje | Cuándo |
  |---|---|---|
  | 401 | `Full authentication is required to access this resource` | Sin token o con un token que el filtro rechaza. |
  | 403 | `Your account has been banned. Reason: {motivo}` / `Your account is suspended until {fecha}. Reason: {motivo}` | El que llama está sancionado (filtro JWT). |
  | 400 | `Invalid value '{valor}' for parameter 'donationId'` | `donationId` no es un número. |
  | 404 | `Donation not found with id: {id}` | La donación no existe. |
  | 400 | `Cannot reply to a donation of a deleted campaign` | La campaña está borrada, también para un ADMIN. |
  | 403 | `Only the campaign owner or an admin can delete this reply` | El que llama no es el dueño ni ADMIN. |
  | 404 | `Donation has no thanks message` | La donación no tiene respuesta. |

- **Qué hace** (`DonationService.deleteDonationThanks`, `@Transactional`):
  1. Obtiene el id del que llama del token.
  2. `validateAndGetDonation` → `validateAndGetDonationCampaign` → `validateIsCampaignOwnerOrAdmin` → `validateHasThanks`.
  3. Pone `thanksMessage = null`, `thanksPrivate = false` y `thankedAt = null`, guarda y mapea.

### `GET /api/donations/my-donations`

- **Acceso:** usuario autenticado, USER o ADMIN (`anyRequest().authenticated()`). Sin token o con token rechazado → `401`; cuenta sancionada → `403` del filtro.
- **Parámetros:** ninguno.
- **Body:** sin body.
- **Respuesta:** `200 OK`.

  ```json
  {
    "message": "Donations retrieved successfully",
    "donations": [ { "...": "DonationResponseDTO" } ],
    "total": 3
  }
  ```

  Ordenadas por `createdAt` descendente, sin paginar. `total` es el tamaño de la lista. Cada donación incluye su `transactionId`, su mensaje completo y la respuesta del dueño, también los privados. Una respuesta en una campaña que después se borró sigue apareciendo, pero ya no se puede borrar. Las donaciones a campañas borradas traen `campaignDeleted = true` y el nombre que tenía la campaña.

- **Errores:**

  | Status | Mensaje | Cuándo |
  |---|---|---|
  | 401 | `Full authentication is required to access this resource` | Sin token o con un token que el filtro rechaza. |
  | 403 | `Your account has been banned. Reason: {motivo}` | El que llama está baneado. |
  | 403 | `Your account is suspended until {fecha}. Reason: {motivo}` | El que llama tiene una suspensión vigente. |

- **Qué hace:**
  1. Obtiene el id del que llama del token.
  2. `DonationRepository.findByDonorIdOrderByCreatedAtDesc`: una sola consulta que filtra por la columna `donor_user_id` y trae con `LEFT JOIN FETCH` el donante y la campaña con su meta y su dueño (son relaciones `EAGER`; sin el fetch, Hibernate haría una consulta extra por cada una). Al ser `LEFT`, una campaña borrada queda en `null` en lugar de sacar la fila.
  3. Junta los ids de las campañas borradas y busca sus nombres en una sola consulta nativa (`CampaignRepository.findNamesByIdsIncludingDeleted`).
  4. Mapea con `DonationMapper.toDTOList(donations, deletedCampaignNames)`.

### `GET /api/donations/campaign/{campaignId}`

- **Acceso:** público (`PUBLIC_GET_URLS` en `SecurityConfig`, solo `GET`). El service resuelve al que llama como opcional (`AuthenticatedUserService.findAuthenticatedUserId`): un token inválido no da `401`, se trata como anónimo. Un usuario sancionado que manda su token recibe el `403` del filtro.
- **Parámetros:**

  | Nombre | Ubicación | Tipo | Obligatorio |
  |---|---|---|---|
  | `campaignId` | path | `Long` | sí |

- **Body:** sin body.
- **Respuesta:** `200 OK`.

  ```json
  {
    "message": "Campaign donations retrieved successfully",
    "donations": [ { "...": "DonationResponseDTO" } ],
    "total": 5
  }
  ```

  Ordenadas por `createdAt` descendente, sin paginar. Para quien no es el dueño, ADMIN ni el donante, `transactionId` va en `null`, un mensaje privado se ve como una donación sin mensaje (`message: null`, `messagePrivate: false`) y una respuesta privada como una donación sin respuesta (`thanksMessage: null`, `thanksPrivate: false`, `thankedAt: null`), según las reglas de [Visibilidad y permisos](#visibilidad-y-permisos). Un donante borrado aparece con `donorUserId` y `donorUserName` en `null`.

- **Errores:**

  | Status | Mensaje | Cuándo |
  |---|---|---|
  | 400 | `Invalid value '{valor}' for parameter 'campaignId'` | `campaignId` no es un número. |
  | 403 | `Your account has been banned. Reason: {motivo}` / `Your account is suspended until {fecha}. Reason: {motivo}` | El que llama manda el token de una cuenta sancionada. |
  | 404 | `Campaign not found with id: {id}` | La campaña no existe, está borrada, es `CLOSED` de otro usuario, o su dueño está borrado o sancionado (para ADMIN, solo si no existe, está borrada o su dueño está borrado). |

- **Qué hace:**
  1. Obtiene el id del que llama si hay token válido, o `null`.
  2. `validateAndGetCampaign(campaignId, callerId)`: la campaña visible para el que llama o `404`.
  3. `DonationRepository.findByCampaignOrderByCreatedAtDesc(campaign)`: una consulta con los donantes (y su `userInfo` y `verificationRequest`) traídos con `LEFT JOIN FETCH`.
  4. Mapea con `DonationMapper.toDTOList`.
  5. Si el que llama no es el dueño ni ADMIN (`canSeePrivateDonationData`), en todas las donaciones que no son suyas pone `transactionId = null`; si el mensaje es privado, `message = null` y `messagePrivate = false`; y si la respuesta es privada, `thanksMessage = null`, `thanksPrivate = false` y `thankedAt = null`. Los mensajes y las respuestas públicos no se tocan.

### `GET /api/donations/campaign/{campaignId}/statistics`

- **Acceso:** público (`PUBLIC_GET_URLS`, solo `GET`), con el que llama resuelto como opcional, igual que el endpoint anterior.
- **Parámetros:**

  | Nombre | Ubicación | Tipo | Obligatorio |
  |---|---|---|---|
  | `campaignId` | path | `Long` | sí |

- **Body:** sin body.
- **Respuesta:** `200 OK`.

  ```json
  {
    "message": "Statistics retrieved successfully",
    "statistics": { "...": "CampaignStatisticsDTO" }
  }
  ```

  `CampaignStatisticsDTO`:

  | Campo | Tipo | Cómo se calcula |
  |---|---|---|
  | `campaignId` | `Long` | El de la ruta. |
  | `campaignName` | `String` | Nombre de la campaña. |
  | `amountGoal` | `BigDecimal` | `goal.amountGoal`; `0` si no hay meta. |
  | `amountRaised` | `BigDecimal` | `goal.amountRaised`, el mismo total que devuelve la campaña; `0` si no hay meta. |
  | `percentageReached` | `BigDecimal` | `amountRaised * 100 / amountGoal`, 2 decimales, `HALF_UP`. `0` si la meta es `<= 0` (evita dividir por cero). Puede superar 100, y por el redondeo puede marcar `100.00` con la meta todavía sin alcanzar (por ejemplo, `99999.99` de `100000.00`). |
  | `isGoalReached` | `Boolean` | `amountRaised >= amountGoal`. Se calcula con los montos, no con el estado: una campaña `CLOSED` que había alcanzado la meta da `true`. Sin meta da `true` (`0 >= 0`). |
  | `status` | `CampaignStatus` | Estado de la meta; `null` si no hay meta. |
  | `totalDonors` | `Long` | Donantes distintos (`COUNT(DISTINCT donorUserId)`) entre las donaciones `COMPLETED`. |
  | `averageDonation` | `BigDecimal` | `amountRaised / totalDonations`, 2 decimales, `HALF_UP`; `0` si no hay donaciones. |
  | `totalDonations` | `Long` | Cantidad de donaciones `COMPLETED`. |

  Los conteos filtran por la columna `campaign_id`, así que incluyen las donaciones de donantes borrados, baneados o suspendidos.

- **Errores:**

  | Status | Mensaje | Cuándo |
  |---|---|---|
  | 400 | `Invalid value '{valor}' for parameter 'campaignId'` | `campaignId` no es un número. |
  | 403 | `Your account has been banned. Reason: {motivo}` / `Your account is suspended until {fecha}. Reason: {motivo}` | El que llama manda el token de una cuenta sancionada. |
  | 404 | `Campaign not found with id: {id}` | Mismos casos que en `GET /api/donations/campaign/{campaignId}`. |

- **Qué hace:**
  1. Obtiene el id del que llama si hay token válido, o `null`.
  2. `validateAndGetCampaign(campaignId, callerId)`.
  3. `countCompletedDonationsByCampaignId` y `countUniqueDonorsByCampaignId` (un `null` se toma como `0`).
  4. Lee meta y recaudado de la meta de la campaña y calcula porcentaje y promedio.
  5. Arma el `CampaignStatisticsDTO` con el builder (no hay mapper: el DTO es un cálculo, no una entidad).

## Clases involucradas

| Capa | Clase |
|---|---|
| Controller | `controller/donation/DonationController.java` |
| Service | `service/donation/DonationService.java` |
| Validation | `validation/donation/DonationValidationService.java`, `validation/common/TextValidationService.java` (texto obligatorio del mensaje y la respuesta) |
| Validation (de campaign) | `validation/campaign/CampaignValidationService.java` (visibilidad), `validation/campaign/CampaignAuthorizationService.java` (`isAdmin`) |
| Service (de campaign) | `service/campaign/CampaignStatusHistoryService.java` |
| Mapper | `mapper/donation/DonationMapper.java` (y `mapper/campaign/CampaignMapper.java`, que crea la meta de cada campaña) |
| Repository | `repository/donation/DonationRepository.java`, `repository/campaign/GoalRepository.java`, `repository/campaign/CampaignRepository.java`, `repository/user/UserRepository.java` |
| Entity | `entity/donation/Donation.java`, `entity/donation/DonationStatus.java`, `entity/campaign/Goal.java`, `entity/campaign/Campaign.java`, `entity/user/UserInfo.java` (`autoThanksMessage`) |
| DTOs | `dtos/donation/CreateDonationDTO.java`, `dtos/donation/DonationCreatedResponseDTO.java`, `dtos/donation/CreateDonationThanksDTO.java`, `dtos/donation/DonationResponseDTO.java`, `dtos/donation/CampaignStatisticsDTO.java` |
| Seguridad | `security/AuthenticatedUserService.java`, `security/SecurityConfig.java`, `security/JwtAuthenticationFilter.java`, `validation/auth/AccountStatusValidationService.java` (mensajes del `403`) |
| Errores | `exception/GlobalExceptionHandler.java`, `exception/MessageException.java`, `exception/ResourceNotFoundException.java`, `exception/ForbiddenActionException.java`, `exception/ConflictException.java` |

`DonationRepository` declara además consultas que hoy no usa ninguna clase: `findByDonor`, `findByDonorOrderByCreatedAtDesc`, `findByCampaign`, `findByStatus` y `findByCampaignIdOrderByCreatedAtDesc`.
