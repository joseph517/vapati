-- Crea un usuario ADMIN directamente en la base de datos, sin pasar por el
-- endpoint POST /api/users/create (que siempre asigna el rol USER).
--
-- Requisitos previos:
--   1. La app debe haber arrancado al menos una vez con el seed de
--      src/main/resources/data.sql aplicado (siembra los roles USER/ADMIN).
--   2. Genera el hash BCrypt de la contrasena con la utilidad standalone:
--        java -cp target/classes:$(./mvnw -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) \
--             com.vaPaTi.vaPaTi.utils.PasswordHashGenerator "miPasswordSegura"
--      y reemplaza el placeholder <BCRYPT_HASH_AQUI> abajo con el resultado.
--
-- Ejecutar contra la BD, por ejemplo:
--   docker exec -it <contenedor_sqlserver> /opt/mssql-tools18/bin/sqlcmd \
--     -S localhost -U sa -P 'TuContrasenaSegura!' -C -d ApiVaPaTiJava -i create-admin-user.sql

DECLARE @adminRoleId BIGINT = (SELECT id FROM roles WHERE name = 'ADMIN');

IF @adminRoleId IS NULL
BEGIN
    RAISERROR ('Rol ADMIN no existe. Arranca la app primero para que data.sql lo siembre.', 16, 1);
    RETURN;
END

INSERT INTO [user] (role_id, is_active, is_verified, created_at, updated_at)
VALUES (@adminRoleId, 1, 1, GETDATE(), GETDATE());

DECLARE @newUserId BIGINT = SCOPE_IDENTITY();

INSERT INTO user_info (user_id, first_name, last_name, email, user_name, password, phone, description, created_at, updated_at)
VALUES (
    @newUserId,
    'Admin',
    'VaPaTi',
    'admin@vapati.com',
    'admin',
    -- Password: TuContrasenaSegura!
    '$2a$10$SriP3hFwrC2kUStX5OSLfeTqW.BlF5BwIPEPdzJnZsg2W.b1nk0yu',
    '0000000000',
    'Administrator account',
    GETDATE(),
    GETDATE()
);
