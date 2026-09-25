# Guía paso a paso: publicar en Docker Hub y desplegar en un servidor

Esta guía es el detalle operativo completo (con troubleshooting) del flujo resumido en las secciones "Publicar la imagen en Docker Hub" y "Desplegar en un servidor" del [`README.md`](README.md). Las secciones 1-6 son el flujo manual, comando por comando. La sección 7 automatiza el build y push (secciones 2-3) con GitHub Actions: cada merge a `main` publica la imagen sola. La sección 8 describe cómo automatizar también el despliegue en el servidor (pendiente).

## 0. Requisitos previos

- Docker instalado y en ejecución.
- Una cuenta en [Docker Hub](https://hub.docker.com/).
- El CLI de Docker autenticado en esta máquina (ver sección 1) — **iniciar sesión en el sitio web de Docker Hub desde el navegador no autentica al Docker CLI**, son cosas independientes.

---

## 1. Autenticar el Docker CLI (`docker login`)

1. Generá un **Access Token** en Docker Hub: `hub.docker.com` → tu perfil → **Account Settings → Security → New Access Token** (nombre libre, ej. `vapati-deploy`, permisos Read/Write).
2. Corré:

   ```bash
   docker login -u <tu-usuario-dockerhub>
   ```

   Cuando pida el password, pegá el **access token** generado (no tu password real de Docker Hub).

### Troubleshooting: `error saving credentials ... pass not initialized`

En Linux sin Docker Desktop corriendo, el credential helper por defecto (`desktop`, que internamente usa `pass`) puede fallar con:

```
error saving credentials: error storing credentials - err: exit status 1, out: `pass not initialized: exit status 1: Error: password store is empty. Try "pass init".`
```

Esto no es un problema de tus credenciales, sino que `pass` (el gestor de contraseñas que usa el credential helper) necesita una clave GPG antes de poder usarse. Se soluciona una sola vez por máquina:

```bash
# 1. Generar una clave GPG sin passphrase (uso local, solo para este propósito)
cat > /tmp/gpg-gen-key.conf <<'EOF'
%no-protection
Key-Type: RSA
Key-Length: 3072
Name-Real: Docker Credential Store
Name-Email: docker-credstore@localhost
Expire-Date: 0
%commit
EOF
gpg --batch --gen-key /tmp/gpg-gen-key.conf
rm /tmp/gpg-gen-key.conf

# 2. Obtener el ID de la clave recién creada
gpg --list-secret-keys --keyid-format=long docker-credstore@localhost
# Copiá el valor que aparece después de "rsa3072/" en la línea "sec"

# 3. Inicializar pass con esa clave
pass init <KEY_ID>
```

Después de esto, repetí `docker login -u <tu-usuario-dockerhub>` — debería guardar las credenciales sin error.

---

## 2. Build de la imagen

Desde la raíz del repo (usa el `Dockerfile` multi-stage: compila con Maven en un stage y copia solo el JAR final a la imagen runtime `eclipse-temurin:21-jre`):

```bash
docker build -t <tu-usuario-dockerhub>/vapati:<tag> .
```

Ejemplo con `latest`:

```bash
docker build -t <tu-usuario-dockerhub>/vapati:latest .
```

---

## 3. Push a Docker Hub

```bash
docker push <tu-usuario-dockerhub>/vapati:<tag>
```

Verificá en `https://hub.docker.com/r/<tu-usuario-dockerhub>/vapati` que el tag aparezca publicado.

---

## 4. Desplegar en un servidor

El servidor **solo necesita** dos archivos — no el repositorio completo:

- `docker-compose.prod.yml`
- `.env` (con los valores reales de producción)

### 4.1. Preparar el `.env` de producción

Copiá `.env.example` como base y completá, como mínimo:

```bash
DB_PASSWORD=<password fuerte, cumple la política de complejidad de SQL Server>
JWT_SECRET=<secreto largo y aleatorio>
CORS_ALLOWED_ORIGINS=http://localhost:3000   # o los orígenes reales cuando exista frontend desplegado
IMAGE_NAME=<tu-usuario-dockerhub>/vapati
IMAGE_TAG=<tag>
SERVER_PORT=
APP_PORT=
DB_PORT=
```

> ⚠️ `JWT_SECRET` es obligatorio y necesita al menos 32 bytes. Si falta, Compose no levanta el contenedor (`JWT_SECRET is required`); si está vacío, es el placeholder de ejemplo (`your-512-bit-secret-key-should-be-long-and-random`) o tiene menos de 32 bytes, la app no arranca. Generalo con `openssl rand -base64 64 | tr -d '\n'`.
>
> `DB_PASSWORD` también es obligatorio: sin él, la app arranca igual usando la password de desarrollo por defecto, que **no es segura para producción**.

### 4.2. Copiar los archivos al servidor

```bash
scp docker-compose.prod.yml .env usuario@servidor:/ruta/de/despliegue/
```

### 4.3. Crear el volumen de la base de datos (solo la primera vez)

`docker-compose.prod.yml` declara el volumen de SQL Server como `external: true`, así que Compose no lo crea solo — hay que crearlo a mano antes del primer `up -d` en cada servidor nuevo:

```bash
docker volume create sql_data_apivapati_java
```

Si te saltás este paso, `up -d` falla con `volume "sql_data_apivapati_java" not found`. Solo hace falta la primera vez: una vez creado, persiste entre reinicios y redeploys.

### 4.4. Descargar la imagen y levantar el stack

En el servidor, dentro de esa carpeta:

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Esto levanta `db_vapati_prod` (SQL Server) y `app_vapati_prod` (la imagen publicada), sin necesidad de tener el código fuente en el servidor. La app queda accesible en el puerto configurado (`APP_PORT`).

### 4.5. Verificar que levantó bien

```bash
docker compose -f docker-compose.prod.yml logs -f app
```

Buscá la línea `Started VaPaTiApplication in X seconds`. Luego probá un endpoint público:

```bash
curl http://localhost:8080/api/categories/list
```

Debería devolver las categorías sembradas por `data.sql` (Technology, Health, Education, Environment, Arts, Sports) — el seed corre igual que en desarrollo, sin pasos adicionales.

---

## 5. Crear el usuario administrador en producción

La imagen runtime no tiene Maven ni `~/.m2`, así que el comando de generación de hash usado en desarrollo no funciona ahí. Se usa el `PropertiesLauncher` de Spring Boot contra el `app.jar` ya empaquetado:

```bash
docker exec app_vapati_prod sh -c "java -Dloader.main=com.vaPaTi.vaPaTi.utils.PasswordHashGenerator -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher '<password-del-admin>'"
```

Esto imprime un hash BCrypt (`$2a$10$....`). Con ese hash:

1. Editá `scripts/create-admin-user.sql` y reemplazá el hash/email/username por los que quieras usar.
2. Copiá y ejecutá el script contra la base de datos de producción:

   ```bash
   docker cp scripts/create-admin-user.sql db_vapati_prod:/tmp/create-admin-user.sql
   docker exec db_vapati_prod /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<DB_PASSWORD del .env>' -C -d ApiVaPaTiJava -i /tmp/create-admin-user.sql
   ```

3. Verificá el login:

   ```bash
   curl -X POST http://<host-del-servidor>:8080/auth/login -H "Content-Type: application/json" -d '{"email":"<email-del-admin>","password":"<password-del-admin>"}'
   ```

   La respuesta debe incluir `"role":"ADMIN"`.

> ⚠️ El script no valida si el admin ya existe: ejecutarlo dos veces sin cambiar el email/username produce un error de clave duplicada. Si necesitás repetirlo, borrá antes el usuario anterior o cambiá esos valores en el script.

---

## 6. Limpieza / rollback

Para bajar el stack de producción sin perder los datos de la base:

```bash
docker compose -f docker-compose.prod.yml down
```

Para bajarlo **y borrar también el volumen de datos** (⚠️ irreversible, borra la base de datos):

```bash
docker compose -f docker-compose.prod.yml down -v
```

---

## 7. Publicación automática con GitHub Actions

El workflow [`.github/workflows/docker-publish.yml`](.github/workflows/docker-publish.yml) reemplaza las secciones 2 y 3 (build y push manual). Ramas:

- **`dev`**: desarrollo diario, igual que hasta ahora.
- **`main`**: lo que está en producción. **Solo se actualiza con un Pull Request `dev → main`**, y cada merge publica una imagen nueva en Docker Hub.

| Evento | Qué hace el workflow | Tags publicados en Docker Hub |
|---|---|---|
| Se abre/actualiza un PR hacia `main` | Corre los tests (`./mvnw -B test`), **no publica** | — |
| Merge (push) a `main` | Tests → build → push | `latest` |
| Push de un tag de git `vX.Y.Z` | Tests → build → push | `X.Y.Z`, `X.Y` |

Si los tests fallan, la imagen **no** se publica. Los tests incluyen los de integración con Testcontainers: los runners de GitHub traen Docker. El workflow le pasa a Maven `-Ddocker.remote.api.version=<versión del daemon del runner>`, porque el `pom.xml` fija la versión de la API de Docker para correr los tests en local, y esa versión puede no coincidir con la del runner. En local no cambia nada.

> **Tag `sha-<commit>` (desactivado)**: el paso `Docker meta` del workflow tiene la línea `# type=sha` comentada. Si la descomentás, cada imagen publicada lleva además un tag `sha-<commit>` (ej. `sha-a1b2c3d`), que sirve para identificar de qué commit salió y para volver a un merge que no tiene versión. La contra: se agrega un tag por cada merge en la lista de Docker Hub. Está desactivado porque las versiones `vX.Y.Z` ya cubren el rollback (sección 7.4).

### 7.1. Configuración inicial (una sola vez)

Hacé estos pasos **en orden**. Los pasos 1-2 tienen que estar listos antes del 4, porque crear `main` ya dispara la primera publicación.

**Paso 1: access token de Docker Hub**

Si ya tenés uno de la sección 1 podés reusarlo, pero conviene uno exclusivo para CI, así lo podés revocar sin afectar tu máquina: `hub.docker.com` → tu perfil → **Account Settings → Personal access tokens → Generate new token**. Nombre: `github-actions-vapati`, permisos **Read & Write**. Copialo: Docker Hub no lo vuelve a mostrar.

**Paso 2: cargar las credenciales en GitHub**

En el repo en GitHub: **Settings → Secrets and variables → Actions**. Esto requiere ser admin del repo.

- Pestaña **Variables** → **New repository variable**:
  - Name: `DOCKERHUB_USERNAME`
  - Value: tu usuario de Docker Hub (ej. `joseph517`). La imagen se publica como `<DOCKERHUB_USERNAME>/vapati`.
- Pestaña **Secrets** → **New repository secret**:
  - Name: `DOCKERHUB_TOKEN`
  - Value: el token del paso 1.

> El usuario va como *variable* y no como *secret* a propósito: GitHub oculta los secrets en los logs como `***`, y el nombre de la imagen quedaría ilegible. El usuario no es sensible; el token sí.

Verificá también en **Settings → Actions → General** que *Actions permissions* permita ejecutar acciones, sin bloquear las de terceros: el workflow usa `docker/*` y `actions/*`.

**Paso 3: subir el workflow a `dev`**

El archivo `.github/workflows/docker-publish.yml` tiene que estar commiteado en `dev` y pusheado (`git push origin dev`).

**Paso 4: crear la rama `main` desde `dev`**

```bash
git fetch origin
git push origin origin/dev:refs/heads/main
```

Esto crea `main` en GitHub apuntando al mismo commit que `dev`. Como es un push a `main`, **dispara la primera publicación**: en la pestaña **Actions** vas a ver el workflow corriendo. Cuando termine en verde, en `https://hub.docker.com/r/<usuario>/vapati/tags` debería aparecer `latest`.

> `dev` sigue siendo la rama por defecto del repo, y está bien: los PRs de features siguen apuntando a `dev`. No hace falta cambiarla.

**Paso 5: proteger `main` (ruleset)**

**Settings → Rules → Rulesets → New ruleset → New branch ruleset**:

1. **Ruleset name**: `proteger-main`.
2. **Enforcement status**: `Active`.
3. **Bypass list**: vacía. Si querés que un admin pueda saltearla en una emergencia, agregá el rol *Repository admin*, pero lo ideal es que nadie la saltee.
4. **Target branches** → **Add target** → **Include by pattern** → `main`.
5. **Rules**, marcar:
   - ✅ **Restrict deletions**: nadie puede borrar `main`.
   - ✅ **Require a pull request before merging**:
     - *Required approvals*: `0` si trabajás solo; `1` si hay otra persona que revise. Con `1`, el autor del PR no puede aprobarse a sí mismo.
     - *Allowed merge methods*: dejá **solo `Merge`** (merge commit), ver la nota de abajo.
   - ✅ **Require status checks to pass**:
     - **Add checks** → buscá `Tests`, que es el nombre del job del workflow. GitHub solo lo sugiere si ya corrió al menos una vez en el repo. Si no aparece, abrí un PR de prueba `dev → main` para que corra y volvé a este paso.
   - ✅ **Block force pushes**.
6. **Create**.

Con esto, un `git push origin main` directo es rechazado, y el botón de merge de un PR queda deshabilitado mientras los tests no pasen.

> **Por qué solo "Merge" y no "Squash" ni "Rebase"**: squash y rebase crean en `main` commits nuevos que no existen en `dev`. En el siguiente PR `dev → main`, GitHub ve historias distintas y aparecen conflictos o commits "duplicados". Con merge commit, `main` siempre es un ancestro de `dev` y cada PR trae exactamente lo nuevo.

### 7.2. Flujo del día a día: publicar a producción

1. Trabajás en `dev` como siempre (features, PRs de features hacia `dev`).
2. Cuando querés publicar: en GitHub, **Pull requests → New pull request** → *base:* `main` ← *compare:* `dev` → **Create pull request**.
3. En el PR corre el check **Tests**. Si falla, arreglás en `dev` y pusheás: el PR se actualiza y el check se vuelve a correr.
4. Con el check en verde → **Merge pull request** (merge commit).
5. El merge dispara el workflow en `main`: tests → build → push de `latest`. Lo seguís en la pestaña **Actions**. Tarda unos minutos, más la primera vez porque no hay caché.
6. Verificás el tag nuevo en Docker Hub.

> Un **Rerun** de un workflow viejo en `main` vuelve a publicar ese código viejo como `latest`. Si necesitás volver atrás, preferí el rollback por tag (sección 7.4).

### 7.3. Versiones con tags de git (`vX.Y.Z`)

`latest` siempre apunta al último merge a `main`, así que no sirve para saber qué versión corre en el servidor ni para volver atrás. Para eso se publican versiones con nombre, creando un **tag de git** sobre un commit de `main`.

**Esquema de versiones ([SemVer](https://semver.org/lang/es/))**: `vMAYOR.MENOR.PARCHE`

- **PARCHE** (`v1.2.0 → v1.2.1`): corrección de bugs, sin cambios en la API.
- **MENOR** (`v1.2.1 → v1.3.0`): funcionalidad nueva compatible (endpoints nuevos, campos nuevos opcionales).
- **MAYOR** (`v1.3.0 → v2.0.0`): cambios que rompen a los clientes (endpoints que cambian o desaparecen, cambios de contrato).

Al subir un número, los de su derecha vuelven a `0`: `1.2.3` + MENOR = `1.3.0`, `1.3.0` + MAYOR = `2.0.0`.

**Cómo decidir el número.** El workflow no decide nada: publica exactamente el número que escribas en el tag. La decisión se toma mirando qué entró a `main` desde la última versión y quedándose con el cambio **más grande**:

```bash
git log --oneline <última-versión>..origin/main      # ej. git log --oneline v1.1.0..origin/main
```

| Si desde la última versión hubo... | Ejemplo en VaPaTi | Sube |
|---|---|---|
| Algo que obliga al frontend/cliente a cambiar | Renombrar o borrar un endpoint, hacer obligatorio un campo de un `Create*DTO`, cambiar la forma de un `*ResponseDTO` | **MAYOR** |
| Algo nuevo que el cliente puede usar, sin romper lo existente | Endpoint nuevo, campo nuevo opcional en un DTO, filtro nuevo en un listado | **MENOR** |
| Solo arreglos o mejoras internas, sin cambios visibles en la API | Fix de una validación, optimización de queries con `JOIN FETCH`, refactor, tests | **PARCHE** |

Si en un mismo release hay un fix y un endpoint nuevo, sube MENOR (gana el más grande). No hace falta taguear cada merge: podés juntar varios merges y crear una sola versión cuando quieras publicar a producción.

**Primera versión**: las versiones anteriores (`v1.0`, `1.0.1`) se publicaron a mano y solo existen como imágenes en Docker Hub; en git no hay tags. Empezá con **`v1.1.0`**. De ahí en adelante, aplicá la tabla.

**Cómo funciona**: el tag **no toma la imagen `latest`**. Etiqueta un **commit**, y el workflow vuelve a construir la imagen desde ese código y la publica como `X.Y.Z` y `X.Y`. Por eso el orden es:

1. Merge del PR `dev → main`: se publica `latest`.
2. Esperá a que ese workflow termine en verde en **Actions**. No es obligatorio, porque la ejecución del tag es independiente, pero si los tests fallaron ahí también van a fallar en la del tag.
3. Creá y subí el tag (comandos de abajo): se publican `X.Y.Z` y `X.Y`.

**Cómo crear una versión**, después de mergear el PR `dev → main`:

```bash
# 1. Traer main actualizado
git fetch origin --tags

# 2. Ver cuál fue la última versión, para decidir el número siguiente
git tag --list 'v*' --sort=-v:refname | head -5

# 3. Crear el tag (anotado, con mensaje) sobre el commit donde está main ahora
git tag -a v1.1.0 origin/main -m "v1.1.0: <resumen de los cambios>"

# 4. Subir el tag: esto dispara el workflow
git push origin v1.1.0
```

Resultado en Docker Hub: `<usuario>/vapati:1.1.0` y `<usuario>/vapati:1.1`.

`origin/main` es "el commit donde está `main` en este momento", por eso el `git fetch` del paso 1 es obligatorio: sin él, tu `origin/main` local puede estar desactualizado y etiquetarías un commit viejo. Si **ya hubo otro merge** y querés versionar uno anterior, usá el hash de ese commit en lugar de `origin/main`:

```bash
git fetch origin
git log --oneline origin/main            # buscar el commit del merge a versionar
git tag -a v1.1.0 a1b2c3d -m "v1.1.0"    # a1b2c3d = hash de ese commit
git push origin v1.1.0
```

Esto publica `1.1.0` y `1.1` con el código de ese commit; `latest` no se mueve.

> **El push del tag no choca con la protección de `main`**: `git push origin v1.1.0` sube solo el tag (`refs/tags/v1.1.0`), no la rama. El ruleset de la sección 7.1 protege `refs/heads/main`, y el tag no mueve `main`: solo nombra un commit que ya llegó ahí por un PR. La excepción es si creaste además un ruleset de **tags** con *Restrict creations*.

La `v` del tag de git **no** aparece en el tag de la imagen: `v1.1.0` → `1.1.0`, que es la convención de Docker. La `v` del tag de git **no** aparece en el tag de la imagen: `v1.1.0` → `1.1.0`, que es la convención de Docker.

Reglas importantes:

- **Tagueá siempre sobre `origin/main`**, nunca sobre `dev` ni sobre un commit local. Así la versión es exactamente lo que se mergeó y se probó en el PR.
- **El formato tiene que ser `vX.Y.Z` con los tres números.** `v1.1` o `version-1` no disparan el workflow.
- **Un tag publicado no se mueve ni se reutiliza.** Si `v1.1.0` salió con un bug, se arregla en `dev`, se mergea a `main` y se publica `v1.1.1`.
- `1.1` (el tag *MAYOR.MENOR*) sí se mueve: siempre apunta al último parche de esa versión. Si en el servidor ponés `IMAGE_TAG=1.1`, recibís parches automáticamente sin saltar a `1.2`.
- Si te equivocaste al crear un tag y **todavía no lo pusheaste**: `git tag -d v1.1.0` y lo volvés a crear.

> El `.env.production` actual usa `IMAGE_TAG=v1.0`, que se publicó a mano con la `v`. Las versiones que publique el workflow no llevan `v` (`1.1.0`), así que al actualizar el servidor usá `IMAGE_TAG=1.1.0`.

### 7.4. Qué tag usar en el servidor y cómo hacer rollback

En el `.env` del servidor (sección 4.1):

- `IMAGE_TAG=1.1.0`: **recomendado para producción.** Versión fija; el servidor solo cambia cuando vos editás este valor.
- `IMAGE_TAG=latest`: siempre el último merge a `main`. Cómodo, pero no sabés qué versión exacta corre y no podés volver atrás cambiando un número.

**Rollback** (volver a la versión anterior si la nueva falla):

```bash
# En el servidor, editar .env: IMAGE_TAG=1.0.0 (la versión anterior que andaba)
docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d app
```

Solo se puede volver a imágenes que tienen versión. Un merge sin tag `vX.Y.Z` queda publicado únicamente como `latest`, y el siguiente merge lo reemplaza. Para poder volver a cualquier merge, activá el tag `sha-<commit>` (nota de la sección 7).

> ⚠️ El rollback de la imagen **no revierte cambios en la base de datos**. Si la versión nueva modificó el esquema (`schema.sql`/`data.sql`), revisá que la versión anterior siga siendo compatible.

### 7.5. Troubleshooting

- **El workflow falla en "Login to Docker Hub" con `unauthorized`**: el token es incorrecto, expiró o es de solo lectura, o `DOCKERHUB_USERNAME` no coincide con el dueño del token. Regenerá el token (Read & Write) y actualizá el secret.
- **Falla en "Docker meta" o el push con `invalid reference format`**: la variable `DOCKERHUB_USERNAME` no existe o se cargó como *secret* en lugar de *variable*. Revisá la pestaña **Variables**.
- **El push de un tag no dispara nada**: el tag no cumple el formato `vX.Y.Z`.
- **Los tests fallan con `Could not find a valid Docker environment`** (en `VaPaTiApplicationTests` y `BankAccountRepositoryTest`): Testcontainers está pidiendo una versión de la API de Docker que el daemon del runner no soporta. Verificá que el paso `Run tests` del workflow siga pasando `-Ddocker.remote.api.version="$(docker version --format '{{.Server.APIVersion}}')"`.
- **No puedo mergear el PR y dice que falta el check `Tests`**: el ruleset exige un check que todavía no corrió en ese PR. Pusheá un commit a `dev` o usá **Re-run jobs** en la pestaña Checks del PR.

---

## 8. (Pendiente) Despliegue automático en el servidor

Hoy el workflow solo **publica** la imagen; el servidor sigue actualizándose a mano (sección 4.4: `pull` + `up -d`). Hay dos formas de automatizar ese último paso.

### Opción A (recomendada): GitHub Actions entra al servidor por SSH

Se agrega un tercer job `deploy` al mismo workflow, que corre después de `publish`:

1. **En el servidor**: crear un usuario de deploy (ej. `deploy`) que pertenezca al grupo `docker`, y generar un par de claves SSH exclusivo para GitHub:

   ```bash
   ssh-keygen -t ed25519 -C "github-actions-vapati" -f ~/.ssh/github_actions -N ""
   cat ~/.ssh/github_actions.pub >> /home/deploy/.ssh/authorized_keys
   ```

2. **En GitHub**: crear un *Environment* `production` (**Settings → Environments → New environment**) con sus secrets: `SSH_HOST`, `SSH_USER`, `SSH_PRIVATE_KEY` (contenido de `~/.ssh/github_actions`) y opcionalmente `SSH_PORT`. En el environment se puede activar **Required reviewers**: el deploy queda en pausa hasta que alguien lo aprueba desde la pestaña Actions. Así la imagen se publica sola, pero el servidor solo se toca con un clic de confirmación.

3. **El job** hace, en esencia:

   ```yaml
   deploy:
     needs: publish
     if: startsWith(github.ref, 'refs/tags/v')   # solo deploya versiones, no cada merge
     runs-on: ubuntu-latest
     environment: production
     steps:
       - name: Deploy por SSH
         uses: appleboy/ssh-action@v1
         with:
           host: ${{ secrets.SSH_HOST }}
           username: ${{ secrets.SSH_USER }}
           key: ${{ secrets.SSH_PRIVATE_KEY }}
           script: |
             cd /ruta/de/despliegue
             TAG='${{ github.ref_name }}'
             sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=${TAG#v}/" .env
             docker compose -f docker-compose.prod.yml pull app
             docker compose -f docker-compose.prod.yml up -d app
             docker image prune -f
   ```

   GitHub reemplaza `${{ github.ref_name }}` por el tag (ej. `v1.1.0`) antes de mandar el script al servidor, y `${TAG#v}` le quita la `v`. El `sed` actualiza `IMAGE_TAG` en el `.env` del servidor con esa versión, así el `.env` siempre refleja lo que corre y el rollback de la sección 7.4 sigue funcionando igual.

   > Este bloque es orientativo: antes de activarlo, revisá la versión vigente de `appleboy/ssh-action`.

**Requisitos**: el servidor tiene que ser accesible por SSH desde internet (los runners de GitHub usan IPs dinámicas), y el puerto SSH no debería quedar con autenticación por password.

### Opción B: Watchtower en el servidor

Watchtower (o una herramienta equivalente) es un contenedor que corre en el servidor, consulta Docker Hub cada N minutos y, si la imagen del tag configurado cambió, la baja y reinicia el contenedor.

- **Ventaja**: no hace falta abrir SSH ni cargar credenciales del servidor en GitHub.
- **Desventajas**: solo tiene sentido con un tag que se mueve (`latest` o `1.1`), no con una versión fija. Actualiza sin aprobación ni aviso. El delay depende del intervalo de consulta. Y el `.env` no refleja qué versión corre.

Antes de elegirla, revisá que el proyecto que uses siga mantenido.

Para un único servidor con releases por versión, la **opción A** da más control (aprobación, versión fija, rollback claro). La B sirve para un entorno de staging que siempre siga a `latest`.
