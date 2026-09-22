# Guía paso a paso: publicar en Docker Hub y desplegar en un servidor

Esta guía es el detalle operativo completo (con troubleshooting) del flujo resumido en las secciones "Publicar la imagen en Docker Hub" y "Desplegar en un servidor" del [`README.md`](README.md). Pensada para ejecutarse manualmente, comando por comando.

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
