# Tests

Guía para correr la suite de tests de VaPaTi, incluyendo los tests de integración que levantan un contenedor MSSQL real vía [Testcontainers](https://testcontainers.com/).

## Tests unitarios (sin Docker)

La mayoría de la suite son tests unitarios (servicios/validaciones contra mocks) y no requieren Docker:

```bash
./mvnw test -Dtest='!BankAccountRepositoryTest,!VaPaTiApplicationTests'
```

## Suite completa (requiere Docker)

`BankAccountRepositoryTest` y `VaPaTiApplicationTests` heredan de `AbstractIntegrationTest`, que levanta un contenedor `mcr.microsoft.com/mssql/server` real con Testcontainers para correr contra `src/test/resources/schema.sql`. Necesitan acceso a un daemon Docker nativo (no Docker Desktop en macOS/Windows — ver `docker --context ls`).

### Localmente, con Maven y Docker nativo instalados

```bash
./mvnw test
```

### Sin Maven instalado, corriendo Maven dentro de un contenedor

Útil en máquinas sin JDK/Maven local, o cuando `DOCKER_HOST` no apunta al contexto nativo por defecto. Se fuerza `--context default` para que Testcontainers conecte al daemon correcto:

**Suite completa:**

```bash
docker --context default run --rm \
  -v "$PWD":/app -w /app \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v maven_repo_cache:/root/.m2 \
  maven:3.9-eclipse-temurin-21 \
  mvn -B test
```

**Una clase específica:**

```bash
docker --context default run --rm \
  -v "$PWD":/app -w /app \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v maven_repo_cache:/root/.m2 \
  maven:3.9-eclipse-temurin-21 \
  mvn -B test -Dtest=BankAccountRepositoryTest
```

Notas:

- Ejecutar desde la raíz del proyecto (`vaPaTi`).
- `-v maven_repo_cache:/root/.m2` cachea las dependencias de Maven entre corridas; Docker crea el volumen solo si no existe.
- Testcontainers crea el contenedor MSSQL, corre los tests contra él y lo destruye al terminar — no toca el volumen persistente `sql_data_apivapati_java` usado por `docker-compose.dev.yml`/`docker-compose.yml`.
- Si `mvn`/`./mvnw test` falla con `Could not find a valid Docker environment`, ver la sección "TestContainers no funciona en Docker" de [`DOCKER-INSTRUCTIONS.md`](DOCKER-INSTRUCTIONS.md).

## Un solo método de test

```bash
./mvnw test -Dtest=DonationServiceTest#createDonation_shouldSucceed
```

## Tests de un paquete completo

```bash
./mvnw test -Dtest="com.vaPaTi.vaPaTi.service.bankAccount.*"
```
