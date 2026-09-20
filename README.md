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

## ⚙️ Instalación y Ejecución

### 1. Clona el repositorio

```bash
git clone https://github.com/joseph517/vaPaTi.git
cd vaPaTi
```

### 2. Asegúrate de tener Docker Desktop instalado y en ejecución

### 3. Levanta la app y la base de datos (SQL Server) con Docker Compose

La forma recomendada para desarrollo (hot reload incluido) es:

```bash
docker-compose -f docker-compose.dev.yml up --build
```

Esto levanta dos contenedores:
- `apivapati_java_db_dev` — SQL Server 2022, expuesto en `localhost:1433` (usuario `sa`, password `TuContrasenaSegura!`, base de datos `ApiVaPaTiJava` — ver `src/main/resources/application-docker.properties`).
- `apivapati_java_app_dev` — la app Spring Boot, expuesta en `localhost:8080`, con hot reload al editar archivos `.java`.

Para más detalle (modo producción, SonarQube, troubleshooting) revisa [`DOCKER-INSTRUCTIONS.md`](DOCKER-INSTRUCTIONS.md).

> Alternativa sin Docker Compose: puedes levantar solo SQL Server con `docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=TuContrasenaSegura!" -p 1433:1433 --name sqlserver -d mcr.microsoft.com/mssql/server:2022-latest` y correr la app localmente con `./mvnw spring-boot:run`, siempre que `src/main/resources/application.properties` apunte a esa misma base de datos.

### 4. Publicar la imagen en Docker Hub

La imagen se construye y publica manualmente (no hay CI/CD automático):

```bash
docker build -t <usuario-dockerhub>/vapati:<tag> .
docker login
docker push <usuario-dockerhub>/vapati:<tag>
```

### 5. Desplegar en un servidor

El servidor **solo necesita** `docker-compose.prod.yml` y un `.env` con los valores reales de producción (`DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `IMAGE_NAME`, `IMAGE_TAG` y puertos) — no el resto del repositorio:

```bash
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

Esto levanta `apivapati_java_db` y `apivapati_java_app` (la imagen publicada), accesible en el puerto configurado (`APP_PORT`). `DB_PASSWORD` y `JWT_SECRET` son obligatorios: sin ellos, la app cae en los valores de desarrollo por defecto, inseguros para producción.

> 📖 Guía paso a paso con más detalle (preparación del `.env`, troubleshooting de `docker login`, verificación completa): [`DOCKER-HUB-DEPLOY.md`](DOCKER-HUB-DEPLOY.md).

### 6. Datos iniciales (roles y categorías)

Al arrancar la app por primera vez, `src/main/resources/data.sql` siembra automáticamente:
- Los roles `USER` y `ADMIN` (tabla `roles`).
- Un set de categorías por defecto (tabla `category`): Technology, Health, Education, Environment, Arts, Sports.

Esto es necesario porque `POST /api/users/create` exige que exista al menos un rol `USER` y al menos una categoría válida (`categoryIds`) — sin este seed, la creación de usuarios falla con `Role not found` o `Categories not found` en una base de datos nueva. El script es idempotente, así que no duplica datos si reinicias la app varias veces. Este seed corre exactamente igual en producción (imagen publicada en Docker Hub), sin pasos adicionales.

### 7. Crear el usuario administrador (directo desde la base de datos)

Por seguridad, **no existe un endpoint para crear usuarios ADMIN** — `POST /api/users/create` siempre crea usuarios con rol `USER`. El admin se crea manualmente insertándolo en la base de datos. El comando para generar el hash BCrypt cambia según el entorno, porque la imagen de producción no tiene Maven ni `~/.m2`:

- En desarrollo (`docker-compose.dev.yml`, imagen con Maven):

  ```bash
  docker exec apivapati_java_app_dev sh -c "cd /app && CP=target/classes:\$(find /root/.m2 -name '*.jar' | tr '\n' ':') && java -cp \"\$CP\" com.vaPaTi.vaPaTi.utils.PasswordHashGenerator 'TuContrasenaSegura'"
  ```

- En producción (`docker-compose.prod.yml`, imagen runtime sin Maven — se usa el `PropertiesLauncher` de Spring Boot contra el `app.jar` ya empaquetado):

  ```bash
  docker exec apivapati_java_app sh -c "java -Dloader.main=com.vaPaTi.vaPaTi.utils.PasswordHashGenerator -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher 'TuContrasenaSegura'"
  ```

Cualquiera de los dos imprime un hash `$2a$10$....`. Con ese hash, edita `scripts/create-admin-user.sql` (reemplaza el hash y, si quieres, el email/username) y ejecútalo contra la base de datos:

```bash
# Desarrollo
docker cp scripts/create-admin-user.sql apivapati_java_db_dev:/tmp/create-admin-user.sql
docker exec apivapati_java_db_dev /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'TuContrasenaSegura!' -C -d ApiVaPaTiJava -i /tmp/create-admin-user.sql

# Producción (usa el DB_PASSWORD real del .env del servidor)
docker cp scripts/create-admin-user.sql apivapati_java_db:/tmp/create-admin-user.sql
docker exec apivapati_java_db /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '<DB_PASSWORD del .env>' -C -d ApiVaPaTiJava -i /tmp/create-admin-user.sql
```

Verifica el login (en producción, cambia `localhost:8080` por el host/puerto real):

```bash
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"admin@vapati.com","password":"TuContrasenaSegura"}'
```

La respuesta debe incluir `"role":"ADMIN"`. El script no valida duplicados: si lo corres dos veces sin cambiar el email/username, falla por clave duplicada.

# Diagramas

<img width="1552" height="1103" alt="image" src="https://github.com/user-attachments/assets/70fe5911-3726-41f1-b4a8-9ba27de8c06b" />

<img width="1008" height="419" alt="dfd_nivel1" src="https://github.com/user-attachments/assets/1f279ee9-161c-4de3-94b7-5defbc0992b3" />

<img width="864" height="960" alt="er_diagram" src="https://github.com/user-attachments/assets/3f5a4b9d-88b2-4576-b982-d70a6e50f413" />

