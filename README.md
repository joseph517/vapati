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

### 4. Datos iniciales (roles y categorías)

Al arrancar la app por primera vez, `src/main/resources/data.sql` siembra automáticamente:
- Los roles `USER` y `ADMIN` (tabla `roles`).
- Un set de categorías por defecto (tabla `category`): Technology, Health, Education, Environment, Arts, Sports.

Esto es necesario porque `POST /api/users/create` exige que exista al menos un rol `USER` y al menos una categoría válida (`categoryIds`) — sin este seed, la creación de usuarios falla con `Role not found` o `Categories not found` en una base de datos nueva. El script es idempotente, así que no duplica datos si reinicias la app varias veces.

### 5. Crear el usuario administrador (directo desde la base de datos)

Por seguridad, **no existe un endpoint para crear usuarios ADMIN** — `POST /api/users/create` siempre crea usuarios con rol `USER`. El admin se crea manualmente insertándolo en la base de datos:

**a) Genera el hash BCrypt de la contraseña que quieras usar** (con la app corriendo en Docker):

```bash
docker exec apivapati_java_app_dev sh -c "cd /app && CP=target/classes:\$(find /root/.m2 -name '*.jar' | tr '\n' ':') && java -cp \"\$CP\" com.vaPaTi.vaPaTi.utils.PasswordHashGenerator 'TuContrasenaSegura'"
```

Esto imprime un hash como `$2a$10$....` — cópialo.

**b) Edita `scripts/create-admin-user.sql`** y reemplaza el placeholder `<BCRYPT_HASH_AQUI>` con el hash generado (y opcionalmente el email/username/nombre por defecto).

**c) Ejecuta el script contra la base de datos:**

```bash
docker cp scripts/create-admin-user.sql apivapati_java_db_dev:/tmp/create-admin-user.sql
docker exec apivapati_java_db_dev /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'TuContrasenaSegura!' -C -d ApiVaPaTiJava -i /tmp/create-admin-user.sql
```

**d) Verifica que el login funciona:**

```bash
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"admin@vapati.com","password":"TuContrasenaSegura!"}'
```

La respuesta debe incluir `"role":"ADMIN"` en el JWT/`userInfo`.

> ⚠️ El script no valida si el admin ya existe: si lo corres dos veces sin cambiar el email/username, crea un duplicado. Si necesitas repetirlo, borra antes el usuario anterior o cambia esos valores en el script.

# Diagramas

<img width="1552" height="1103" alt="image" src="https://github.com/user-attachments/assets/70fe5911-3726-41f1-b4a8-9ba27de8c06b" />

<img width="1008" height="419" alt="dfd_nivel1" src="https://github.com/user-attachments/assets/1f279ee9-161c-4de3-94b7-5defbc0992b3" />

<img width="864" height="960" alt="er_diagram" src="https://github.com/user-attachments/assets/3f5a4b9d-88b2-4576-b982-d70a6e50f413" />

