# 🚀 VaPaTi

**VaPaTi** es una aplicación desarrollada en Java con Spring boot, que permite a los usuarios registrarse, realizar campañas, compartir publicaciones, seguir a usuarios, suscribirse a usuarios, realizar y recibir donaciones.

---

## 🎯 Características Principales

- 🧾 Registro de usuarios
- 💰 Realizar o recibir donaciones
- 🔐 Aprobación automática simulada
- 📊 Seguir a usuarios
- 🏦 Vincular cuentas bancarias al usuario

---

## 🛠️ Tecnologías Usadas
- Java 21
- Spring Boot 3.2.5
- Spring Boot Web
- Spring Boot Thymeleaf
- Spring Boot Data JPA
- Spring Boot Security
- Spring Boot DevTools
- Spring Boot Starter Test
- JWT (JJWT)
- Springdoc OpenAPI (Swagger)
- Microsoft SQL Server
- JDBC Driver para SQL Server (mssql-jdbc)
- JetBrains Annotations
- Maven – Gestión de dependencias y construcción
- Docker – Utilizado para levantar la base de datos (SQL Server) en contenedor

---

## ⚙️ Instalación y ejecución en desarrollo

### 1. Clona el repositorio

```bash
git clone https://github.com/joseph517/vaPaTi.git
cd vaPaTi
```

### 2. Asegúrate de tener Docker Desktop instalado y en ejecución

### 3. Levanta la app y la base de datos (SQL Server) con Docker Compose

La forma recomendada para desarrollo (hot reload incluido) es:

```bash
docker compose -f docker-compose.dev.yml up -d
```

La primera vez (o después de actualizar a una versión que cambie `Dockerfile.dev`) hay que construir la imagen:

```bash
docker compose -f docker-compose.dev.yml up -d --build
```

Esto levanta dos contenedores:
- `db_vapati_dev` — SQL Server 2022, expuesto en `localhost:1433` (usuario `sa`, password `<DB_PASSWORD del .env>`, base de datos `ApiVaPaTiJava` — ver `src/main/resources/application-docker.properties`).
- `app_vapati_dev` — la app Spring Boot, expuesta en `localhost:8080`, con hot reload.

Cómo funciona el hot reload:
- `scripts/dev-hot-reload.sh` no se ejecuta a mano: es el comando con el que arranca `app_vapati_dev`, y vigila los archivos mientras el contenedor esté vivo.
- Al guardar un archivo en `src/main` (código o recursos), la app se recompila y se reinicia sola en unos segundos. El contenedor no se reinicia.
- Si cambias `pom.xml`, aplícalo con `docker compose -f docker-compose.dev.yml restart app-dev`: al arrancar descarga solo las dependencias nuevas.
- Si la compilación falla, la versión anterior sigue corriendo. El error se ve en `docker compose -f docker-compose.dev.yml logs -f app-dev` (banner `COMPILATION FAILED`), y el contenedor aparece `unhealthy` en `docker ps` hasta que lo corriges y guardas.
- `--build` solo hace falta si cambia `Dockerfile.dev` o `scripts/dev-hot-reload.sh`.

> Alternativa sin Docker Compose: puedes levantar solo SQL Server con `docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=<tu-password>" -p 1433:1433 --name sqlserver -d mcr.microsoft.com/mssql/server:2022-latest` y correr la app localmente con `./mvnw spring-boot:run`, siempre que `src/main/resources/application.properties` apunte a esa misma base de datos (con `DB_PASSWORD=<tu-password>` en el entorno). `./mvnw spring-boot:run` también necesita `JWT_SECRET` exportado en el entorno, con al menos 32 bytes: por ejemplo `export JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')`. Sin él, la app no arranca.

### 4. Datos iniciales (roles y categorías)

Al arrancar la app por primera vez, `src/main/resources/data.sql` siembra automáticamente:
- Los roles `USER` y `ADMIN` (tabla `roles`).
- Un set de categorías por defecto (tabla `category`): Technology, Health, Education, Environment, Arts, Sports.

Esto es necesario porque `POST /api/users/create` exige que exista al menos un rol `USER` y al menos una categoría válida (`categoryIds`) — sin este seed, la creación de usuarios falla con `Role not found` o `Categories not found` en una base de datos nueva. El script es idempotente, así que no duplica datos si reinicias la app varias veces. Este mismo seed corre igual en producción, sin pasos adicionales.

### 5. Crear el usuario administrador (desarrollo)

Por seguridad, **no existe un endpoint para crear usuarios ADMIN** — `POST /api/users/create` siempre crea usuarios con rol `USER`. El admin se crea manualmente insertándolo en la base de datos:

```bash
# 1. Generar el hash BCrypt del password (imagen de desarrollo, incluye Maven)
docker exec app_vapati_dev sh -c "cd /app && CP=target/classes:\$(find /root/.m2 -name '*.jar' | tr '\n' ':') && java -cp \"\$CP\" com.vaPaTi.vaPaTi.utils.PasswordHashGenerator '<password-del-admin>'"

# 2. Con el hash impreso ($2a$10$....), edita scripts/create-admin-user.sql (hash, email, username) y ejecútalo
docker cp scripts/create-admin-user.sql db_vapati_dev:/tmp/create-admin-user.sql
docker exec db_vapati_dev /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<DB_PASSWORD del .env>' -C -d ApiVaPaTiJava -i /tmp/create-admin-user.sql

# 3. Verificar el login
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"<email-del-admin>","password":"<password-del-admin>"}'
```

La respuesta debe incluir `"role":"ADMIN"`. El script no valida duplicados: si lo corres dos veces sin cambiar el email/username, falla por clave duplicada.

## Tests

Guía completa (unitarios, integración con Testcontainers/MSSQL, cómo correr una clase o un método específico) en [`TESTING.md`](TESTING.md).

## 🚀 Ejecución en producción

El flujo de producción es: build de la imagen → publicarla en Docker Hub → desplegarla en un servidor con solo `docker-compose.prod.yml` + `.env` → crear el usuario admin ahí. La guía completa y autocontenida, paso a paso, con troubleshooting está en [`DOCKER-HUB-DEPLOY.md`](DOCKER-HUB-DEPLOY.md) — esta sección es solo el resumen y el orden en que se hace.

### 6. Publicar la imagen en Docker Hub

Automático con GitHub Actions ([`.github/workflows/docker-publish.yml`](.github/workflows/docker-publish.yml)): `main` solo se actualiza con un PR `dev → main` (protegida, con los tests como check obligatorio). Cada merge publica `<usuario-dockerhub>/vapati:latest` y `:sha-<commit>`, y cada tag de git `vX.Y.Z` publica `:X.Y.Z` y `:X.Y`:

```bash
git fetch origin
git tag -a v1.1.0 origin/main -m "v1.1.0: <resumen>"
git push origin v1.1.0
```

Configuración inicial (credenciales de Docker Hub en GitHub, creación de `main`, ruleset de protección), esquema de versiones y rollback: ver sección 7 de [`DOCKER-HUB-DEPLOY.md`](DOCKER-HUB-DEPLOY.md). El build y push manuales siguen disponibles (secciones 1-3).

### 7. Desplegar en un servidor

El servidor **solo necesita** `docker-compose.prod.yml` y un `.env` con los valores reales de producción (`DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `IMAGE_NAME`, `IMAGE_TAG` y puertos) — no el resto del repositorio:

```bash
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

Esto levanta `db_vapati_prod` y `app_vapati_prod` (la imagen publicada), accesible en el puerto configurado (`APP_PORT`). `JWT_SECRET` es obligatorio y necesita al menos 32 bytes: sin él, con el placeholder de ejemplo o con uno más corto, la app no arranca (generalo con `openssl rand -base64 64 | tr -d '\n'`). `DB_PASSWORD` también es obligatorio: sin él, la app cae en la password de desarrollo por defecto, insegura para producción.

Detalle completo (preparación del `.env`, copiar los archivos al servidor, verificación de que levantó bien): ver sección 4 de [`DOCKER-HUB-DEPLOY.md`](DOCKER-HUB-DEPLOY.md).

### 8. Crear el usuario administrador (producción)

Mismo flujo que en desarrollo, pero la imagen runtime no tiene Maven ni `~/.m2`, así que el hash se genera con el `PropertiesLauncher` de Spring Boot contra el `app.jar` ya empaquetado:

```bash
docker exec app_vapati_prod sh -c "java -Dloader.main=com.vaPaTi.vaPaTi.utils.PasswordHashGenerator -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher '<password-del-admin>'"
```

Con el hash, completa `scripts/create-admin-user.sql` y ejecútalo contra `db_vapati_prod` (en vez de `db_vapati_dev`), igual que en el paso 5. Detalle completo y verificación de login: ver sección 5 de [`DOCKER-HUB-DEPLOY.md`](DOCKER-HUB-DEPLOY.md).

# Diagramas

<img width="1552" height="1103" alt="image" src="https://github.com/user-attachments/assets/70fe5911-3726-41f1-b4a8-9ba27de8c06b" />

<img width="1008" height="419" alt="dfd_nivel1" src="https://github.com/user-attachments/assets/1f279ee9-161c-4de3-94b7-5defbc0992b3" />

<img width="864" height="960" alt="er_diagram" src="https://github.com/user-attachments/assets/3f5a4b9d-88b2-4576-b982-d70a6e50f413" />

