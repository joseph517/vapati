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

# Clona el repositorio

```
git clone https://github.com/joseph517/vaPaTi.git
```

cd vaPaTi

## Asegúrate de tener Docker instalado y en ejecución
## Levanta la base de datos (SQL Server) con Docker
```
- docker run -e "ACCEPT_EULA=Y" -e "SA_PASSWORD=YourPassword123" \
   - p 1433:1433 --name sqlserver \
   - d mcr.microsoft.com/mssql/server:2022-latest
```

## Asegúrate de que la configuración de tu aplicación (application.properties o .yml), apunte al contenedor de SQL Server con los mismos datos de conexión

## Compila y ejecuta la aplicación Spring Boot
```
- ./mvnw spring-boot:run
```

## Si usas application.properties, asegúrate de tener algo así:
```
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=vaPaTi
spring.datasource.username=sa
spring.datasource.password=YourPassword123
spring.jpa.hibernate.ddl-auto=update

```

# Diagramas

<img width="1552" height="1103" alt="image" src="https://github.com/user-attachments/assets/70fe5911-3726-41f1-b4a8-9ba27de8c06b" />

<img width="1008" height="419" alt="dfd_nivel1" src="https://github.com/user-attachments/assets/1f279ee9-161c-4de3-94b7-5defbc0992b3" />

<img width="864" height="960" alt="er_diagram" src="https://github.com/user-attachments/assets/3f5a4b9d-88b2-4576-b982-d70a6e50f413" />

