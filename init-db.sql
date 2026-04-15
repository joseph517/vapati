IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'ApiVaPaTiJava')
BEGIN
    CREATE DATABASE ApiVaPaTiJava;
END
GO