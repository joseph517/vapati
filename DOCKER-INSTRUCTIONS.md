# Instrucciones de Docker para VaPaTi

Este documento explica cómo ejecutar la aplicación VaPaTi usando Docker con Java 21.

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Modos de Ejecución con Perfiles](#modos-de-ejecución-con-perfiles)
3. [Modo Desarrollo vs Modo Producción](#modo-desarrollo-vs-modo-producción)
4. [Comandos Modo Desarrollo (Hot Reload)](#comandos-modo-desarrollo-hot-reload)
5. [Comandos Modo Producción](#comandos-modo-producción)
6. [SonarQube Analysis (Opcional)](#sonarqube-analysis-opcional---solo-modo-desarrollo)
7. [Troubleshooting](#troubleshooting)

---

## Requisitos Previos

- Docker Desktop instalado y en ejecución
- El volumen `sql_data_apivapati_java` debe existir (si ya lo tienes, perfecto; si no, se creará automáticamente)
- Al menos 4 GB de RAM para desarrollo normal
- Al menos 8 GB de RAM si usas SonarQube

### Verificar que el volumen existe

```bash
docker volume ls | grep sql_data_apivapati_java
```

Si no existe, créalo:

```bash
docker volume create sql_data_apivapati_java
```

---

## Modos de Ejecución con Perfiles

VaPaTi tiene **3 modos** de ejecución en Docker:

| Modo | Comando | RAM | Servicios | Uso |
|------|---------|-----|-----------|-----|
| **Producción** | `docker-compose up` | ~1GB | SQL + App (optimizado) | Testing, Deploy |
| **Desarrollo** | `docker-compose -f docker-compose.dev.yml up` | ~2GB | SQL + App (hot reload) | Desarrollo diario |
| **Dev + SonarQube** | `docker-compose -f docker-compose.dev.yml --profile sonar up` | ~4GB | Todo | Análisis de código |

### ¿Cuándo usar cada modo?

**Modo Desarrollo (sin SonarQube)** - Uso diario ⭐ Recomendado
- ✅ Consume solo ~2GB RAM
- ✅ Inicia en 30-60 segundos
- ✅ Hot reload habilitado
- ✅ Perfecto para desarrollo del día a día
- ❌ Sin análisis de código

**Modo Desarrollo + SonarQube** - Cuando necesites analizar código
- ✅ Análisis completo de calidad
- ✅ Hot reload habilitado
- ✅ Cobertura de tests con JaCoCo
- ❌ Consume ~4GB RAM
- ❌ Inicia en 2-3 minutos (SonarQube tarda)

**Modo Producción** - Testing final
- ✅ Imagen optimizada
- ✅ Solo ~1GB RAM
- ✅ Inicio rápido
- ❌ Sin hot reload (debes rebuild al cambiar código)
- ❌ Sin SonarQube

---

## Modo Desarrollo vs Modo Producción

VaPaTi tiene **dos configuraciones de Docker**:

### 🔥 Modo Desarrollo (docker-compose.dev.yml)
- ✅ **Hot Reload**: Los cambios en archivos `.java` se reflejan automáticamente (5-15 segundos)
- ✅ **Debug Remoto**: Puerto 5005 habilitado para debugger
- ✅ **No necesitas reconstruir** al cambiar código
- ❌ Más pesado: ~1-2 GB RAM
- ❌ Más lento al iniciar: 1-2 minutos primera vez
- **Úsalo para**: Desarrollo activo

### 🚀 Modo Producción (docker-compose.yml)
- ✅ **Imagen optimizada**: Solo JRE + JAR (~200 MB)
- ✅ **Inicio rápido**: 20-30 segundos
- ✅ **Menos recursos**: ~512 MB RAM
- ❌ **Sin hot reload**: Debes reconstruir cada cambio
- **Úsalo para**: Testing, CI/CD, Deploy

---

## Comandos Modo Desarrollo (Hot Reload)

### 1. Iniciar en Modo Desarrollo (Primera vez)

**SIN SonarQube (recomendado para desarrollo diario - 2GB RAM):**

```bash
docker-compose -f docker-compose.dev.yml up --build
```

**CON SonarQube (para análisis de código - 4GB RAM):**

```bash
docker-compose -f docker-compose.dev.yml --profile sonar up --build
```

Este comando:
- Construye la imagen de desarrollo con Maven y Java 21
- Inicia SQL Server en el puerto 1433
- Inicia la aplicación Spring Boot en el puerto 8080
- Habilita hot reload y debug en puerto 5005
- Monta tu código fuente como volumen
- **(Con --profile sonar)** Inicia PostgreSQL y SonarQube en puerto 9000
- Espera a que SQL Server esté listo antes de iniciar la app

### 2. Iniciar la aplicación (después de la primera vez)

```bash
docker-compose -f docker-compose.dev.yml up
```

No necesitas `--build` si no cambiaste el `pom.xml` o el `Dockerfile.dev`.

### 3. Iniciar en segundo plano (detached mode)

```bash
docker-compose -f docker-compose.dev.yml up -d
```

### 4. Ver logs en tiempo real

```bash
# Ver logs de todos los servicios
docker-compose -f docker-compose.dev.yml logs -f

# Ver solo logs de la aplicación
docker-compose -f docker-compose.dev.yml logs -f app-dev

# Ver solo logs de la base de datos
docker-compose -f docker-compose.dev.yml logs -f sqlserver
```

### 5. Detener la aplicación

```bash
docker-compose -f docker-compose.dev.yml down
```

### 6. Reconstruir la imagen (después de cambiar pom.xml)

```bash
docker-compose -f docker-compose.dev.yml up --build
```

### 7. Reiniciar solo la aplicación

```bash
docker-compose -f docker-compose.dev.yml restart app-dev
```

---

## Comandos Modo Producción

### 1. Iniciar en Modo Producción (Primera vez)

```bash
docker-compose up --build
```

Este comando:
- Construye la imagen optimizada (multi-stage build)
- Inicia SQL Server en el puerto 1433
- Inicia la aplicación Spring Boot en el puerto 8080
- Espera a que SQL Server esté listo antes de iniciar la app

### 2. Iniciar la aplicación (después de la primera vez)

```bash
docker-compose up
```

No necesitas `--build` si no cambiaste el código o el `Dockerfile`.

### 3. Iniciar en segundo plano (detached mode)

```bash
docker-compose up -d
```

### 4. Ver logs en tiempo real

```bash
# Ver logs de todos los servicios
docker-compose logs -f

# Ver solo logs de la aplicación
docker-compose logs -f app

# Ver solo logs de la base de datos
docker-compose logs -f sqlserver
```

### 5. Detener la aplicación

```bash
docker-compose down
```

### 6. Detener y eliminar volúmenes (CUIDADO: Borra la base de datos)

```bash
docker-compose down -v
```

### 7. Reconstruir la imagen (después de cambiar código)

```bash
docker-compose up --build
```

### 8. Reiniciar solo la aplicación

```bash
docker-compose restart app
```

---

## Hot Reload (Recarga Automática) - Solo Modo Desarrollo

El hot reload **solo funciona en modo desarrollo** (`docker-compose.dev.yml`).

La aplicación está configurada con `spring-boot-devtools` para hot reload:

- ✅ **Archivos .java**: Los cambios se detectan automáticamente y la aplicación se reinicia (5-15 segundos)
- ✅ **Archivos .properties**: Los cambios se aplican al reiniciar la app
- ❌ **pom.xml**: Debes reconstruir la imagen con `docker-compose -f docker-compose.dev.yml up --build`

### Cómo funciona:

1. Edita un archivo `.java` en tu IDE (ej: `src/main/java/com/vaPaTi/vaPaTi/service/ReportService.java`)
2. Guarda el archivo
3. Espera 5-15 segundos (verás en los logs: "Restarting due to changes...")
4. La aplicación se reinicia automáticamente en el contenedor
5. Los cambios están disponibles en http://localhost:8080

### Ejemplo práctico:

```bash
# Terminal 1: Inicia modo desarrollo
docker-compose -f docker-compose.dev.yml up

# Terminal 2: Ver logs en tiempo real
docker-compose -f docker-compose.dev.yml logs -f app-dev

# Ahora edita cualquier archivo .java, guárdalo y verás:
# "Restarting due to changes to /app/src/main/java/..."
# "Started VaPaTiApplication in X seconds"
```

**Nota**: Si usas modo producción (`docker-compose.yml`), NO tendrás hot reload y deberás hacer `docker-compose up --build` cada vez que cambies código.

## Acceder a la Aplicación

- **API REST**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **SQL Server**: localhost:1433
  - Usuario: `sa`
  - Contraseña: `TuContrasenaSegura!`
  - Base de datos: `ApiVaPaTiJava`

## Troubleshooting

### Error: "Cannot connect to database"

**Modo Desarrollo:**
```bash
docker-compose -f docker-compose.dev.yml ps
docker-compose -f docker-compose.dev.yml logs sqlserver
```

**Modo Producción:**
```bash
docker-compose ps
docker-compose logs sqlserver
```

### Error: "Port 8080 already in use"

Detén cualquier otra aplicación usando el puerto 8080 o cambia el puerto:

**docker-compose.dev.yml:**
```yaml
app-dev:
  ports:
    - "8081:8080"  # Cambia 8080 por otro puerto
```

**docker-compose.yml:**
```yaml
app:
  ports:
    - "8081:8080"  # Cambia 8080 por otro puerto
```

### Error: "Port 1433 already in use"

Si tienes otro SQL Server corriendo, detenlo o cambia el puerto:

```yaml
sqlserver:
  ports:
    - "1434:1433"  # Cambia 1433 por otro puerto
```

Y actualiza `application-docker.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://sqlserver:1434;databaseName=ApiVaPaTiJava;trustServerCertificate=true
```

### Hot reload no funciona (Modo Desarrollo)

1. Verifica que estás usando `docker-compose.dev.yml`:
```bash
docker-compose -f docker-compose.dev.yml ps
```

2. Verifica que los volúmenes están montados:
```bash
docker inspect apivapati_java_app_dev | grep -A 10 Mounts
```

3. Verifica los logs para ver si detecta cambios:
```bash
docker-compose -f docker-compose.dev.yml logs -f app-dev
```

4. Si cambias `pom.xml`, reconstruye:
```bash
docker-compose -f docker-compose.dev.yml up --build
```

### Limpiar y empezar de cero

**Modo Desarrollo:**
```bash
# Detener todo
docker-compose -f docker-compose.dev.yml down

# Eliminar imágenes
docker-compose -f docker-compose.dev.yml down --rmi all

# Eliminar volúmenes (CUIDADO: Borra la BD)
docker-compose -f docker-compose.dev.yml down -v

# Reconstruir todo
docker-compose -f docker-compose.dev.yml up --build
```

**Modo Producción:**
```bash
# Detener todo
docker-compose down

# Eliminar imágenes
docker-compose down --rmi all

# Eliminar volúmenes (CUIDADO: Borra la BD)
docker-compose down -v

# Reconstruir todo
docker-compose up --build
```

### Ver contenedores en ejecución

```bash
docker ps
```

### Entrar al contenedor de la aplicación

**Modo Desarrollo:**
```bash
docker exec -it apivapati_java_app_dev bash
```

**Modo Producción:**
```bash
docker exec -it apivapati_java_app bash
```

### Entrar al contenedor de SQL Server

**Modo Desarrollo:**
```bash
docker exec -it apivapati_java_db_dev bash
```

**Modo Producción:**
```bash
docker exec -it apivapati_java_db bash
```

### Cambiar entre Modo Desarrollo y Producción

```bash
# Detener desarrollo
docker-compose -f docker-compose.dev.yml down

# Iniciar producción (usa la misma BD)
docker-compose up

# O viceversa
docker-compose down
docker-compose -f docker-compose.dev.yml up
```

**Nota**: Ambos modos usan el mismo volumen `sql_data_apivapati_java`, por lo que tus datos persisten.

## Notas Importantes

1. **Perfil de Spring**: La aplicación usa el perfil `docker` automáticamente (definido en `docker-compose.yml`)
2. **Persistencia**: Los datos de SQL Server se guardan en el volumen `sql_data_apivapati_java` (no se pierden al reiniciar)
3. **Memoria**: El contenedor Java está configurado para usar máximo 512MB de RAM
4. **Network**: Ambos contenedores están en la red `vapati-network` y pueden comunicarse entre sí

## Desarrollo Local vs Docker

- **Local (sin Docker)**: Usa `./mvnw spring-boot:run` con `application.properties`
- **Docker**: Usa `docker-compose up` con `application-docker.properties`

Puedes tener ambos entornos configurados simultáneamente sin conflictos.

---

## SonarQube Analysis (Opcional - Solo Modo Desarrollo)

⚠️ **Importante**: SonarQube es OPCIONAL. Solo inícialo cuando necesites analizar el código.

El modo desarrollo puede incluir **SonarQube Server** para análisis de calidad de código usando perfiles.

### Activar SonarQube

Para usar SonarQube, debes iniciar el modo desarrollo con el perfil `sonar`:

```bash
# Detener contenedores actuales (si están corriendo)
docker-compose -f docker-compose.dev.yml down

# Iniciar CON SonarQube
docker-compose -f docker-compose.dev.yml --profile sonar up
```

### Servicios Incluidos

**Sin perfil sonar** (`docker-compose -f docker-compose.dev.yml up`):
1. **SQL Server** (puerto 1433) - Base de datos de la aplicación
2. **Spring Boot App** (puerto 8080) - Tu aplicación

**Con perfil sonar** (`docker-compose -f docker-compose.dev.yml --profile sonar up`):
1. **SQL Server** (puerto 1433) - Base de datos de la aplicación
2. **Spring Boot App** (puerto 8080) - Tu aplicación
3. **PostgreSQL** (interno) - Base de datos para SonarQube
4. **SonarQube Server** (puerto 9000) - Servidor de análisis de código

### Primera Configuración de SonarQube

**1. Espera a que SonarQube esté listo (1-2 minutos):**

```bash
# Verifica los logs
docker logs apivapati_sonarqube -f
```

Espera a ver: `SonarQube is operational`

**2. Accede a SonarQube:**

Abre http://localhost:9000 en tu navegador

**Credenciales por defecto:**
- Usuario: `admin`
- Contraseña: `admin`

**3. Cambiar contraseña (obligatorio en primer login):**

SonarQube te pedirá cambiar la contraseña al primer inicio.

**4. Generar token (opcional - ya hay uno configurado):**

Si necesitas generar un nuevo token:
- Ir a: **Account → Security → Generate Tokens**
- Nombre: `vapati-analysis`
- Tipo: `Global Analysis Token`
- Copiar el token generado y actualizar el script `run-sonar-analysis.bat`

### Ejecutar Análisis de Código

**Opción 1: Usando script (Recomendado):**

```bash
# Windows
./run-sonar-analysis.bat

# Linux/Mac
chmod +x run-sonar-analysis.sh
./run-sonar-analysis.sh
```

**Opción 2: Comando directo:**

```bash
docker exec -it apivapati_java_app_dev ./mvnw clean verify sonar:sonar -Dsonar.host.url=http://sonarqube:9000 -Dsonar.token=sqp_329dd373dc96eea2d6e421fe9746cc6c0eab69db
```

**Opción 3: Desde dentro del contenedor:**

```bash
# Entrar al contenedor
docker exec -it apivapati_java_app_dev bash

# Ejecutar análisis
./mvnw clean verify sonar:sonar -Dsonar.host.url=http://sonarqube:9000 -Dsonar.token=sqp_329dd373dc96eea2d6e421fe9746cc6c0eab69db
```

### Ver Resultados del Análisis

1. Espera a que el análisis termine (puede tardar 2-5 minutos)
2. Abre http://localhost:9000
3. Verás el proyecto **"vaPaTi"** con las estadísticas de código

### Qué se Analiza

- ✅ **Cobertura de código**: Usando JaCoCo
- ✅ **Code Smells**: Problemas de mantenibilidad
- ✅ **Bugs**: Posibles errores en el código
- ✅ **Vulnerabilidades**: Problemas de seguridad
- ✅ **Duplicación**: Código duplicado

### Exclusiones Configuradas

Las siguientes carpetas están excluidas del análisis (configurado en `pom.xml`):

- `controller/` - Controladores REST
- `dtos/` - DTOs
- `entity/` - Entidades JPA
- `exception/` - Excepciones custom
- `mapper/` - Mappers
- `repository/` - Repositorios JPA
- `security/` - Configuración de seguridad
- `utils/` - Utilidades
- `VaPaTiApplication.java` - Clase principal

**Solo se analiza**: Services y Validation (lógica de negocio)

### Detener SonarQube

SonarQube se detiene automáticamente cuando bajas el stack de desarrollo:

```bash
docker-compose -f docker-compose.dev.yml down
```

### Reiniciar Solo SonarQube

```bash
docker restart apivapati_sonarqube
```

### Ver Logs de SonarQube

```bash
docker logs apivapati_sonarqube -f
```

### Recursos Utilizados

**RAM total del stack completo (modo desarrollo con SonarQube):**
- SQL Server: ~512 MB
- Spring Boot App (dev): ~1-2 GB
- PostgreSQL: ~128 MB
- SonarQube: ~2 GB
- **Total: ~4 GB RAM**

**Espacio en disco:**
- Imágenes Docker: ~1 GB
- Volúmenes (datos): ~1-2 GB

### Troubleshooting SonarQube

**Error: "SonarQube server [http://sonarqube:9000] can not be reached"**

1. Verifica que SonarQube esté corriendo:
```bash
docker ps | grep sonarqube
```

2. Verifica los logs:
```bash
docker logs apivapati_sonarqube
```

3. Espera a que SonarQube esté completamente iniciado (1-2 minutos)

**Error: "Insufficient privileges"**

El token puede haber expirado. Genera uno nuevo en SonarQube:
- http://localhost:9000 → Account → Security → Generate Tokens
- Actualiza el token en `run-sonar-analysis.bat` y `run-sonar-analysis.sh`

**SonarQube no inicia:**

```bash
# Ver logs detallados
docker logs apivapati_sonarqube -f

# Reiniciar SonarQube
docker restart apivapati_sonarqube

# Si persiste, eliminar volúmenes y empezar de cero
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up
```

**Puerto 9000 ya en uso:**

Si tienes otro SonarQube local corriendo, detenlo primero:
- Cierra `StartSonar.bat` si lo tienes abierto
- O cambia el puerto en `docker-compose.dev.yml`:
  ```yaml
  sonarqube:
    ports:
      - "9001:9000"  # Cambia a otro puerto
  ```

### TestContainers no funciona en Docker

Si ves errores como `"Could not find a valid Docker environment"` al ejecutar tests:

**Causa:** TestContainers necesita acceso al Docker daemon para crear contenedores temporales durante los tests.

**Solución:**

1. Verifica que el socket de Docker esté montado en `docker-compose.dev.yml`:
```yaml
app-dev:
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
```

2. Verifica que Docker Desktop esté corriendo en Windows

3. Reinicia los contenedores:
```bash
docker-compose -f docker-compose.dev.yml down
docker-compose -f docker-compose.dev.yml --profile sonar up
```

**Qué hace TestContainers:**
- Crea contenedores SQL Server temporales durante los tests
- Los contenedores se eliminan automáticamente al terminar
- Necesario para tests de repositorios (`BankAccountRepositoryTest`, etc.)

**Nota de seguridad:**
- El socket de Docker da acceso completo al daemon
- Solo usar en desarrollo local
- NO exponer en producción

---
