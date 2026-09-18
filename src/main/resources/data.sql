-- Seed inicial de roles y categorias.
-- Se ejecuta en cada arranque (spring.sql.init.mode=always); los INSERT son
-- idempotentes via IF NOT EXISTS para que no falle ni duplique datos en reinicios.
--
-- Nota: la tabla de categorias se llama "category" (singular), no "categories":
-- la entidad Category no tiene @Table explicito, asi que Hibernate usa el
-- nombre de la clase en snake_case tal cual (sin pluralizar).

IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER')
    INSERT INTO roles (name) VALUES ('USER');

IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN')
    INSERT INTO roles (name) VALUES ('ADMIN');

IF NOT EXISTS (SELECT 1 FROM category WHERE name = 'Technology')
    INSERT INTO category (name, description) VALUES ('Technology', 'Technology related campaigns');

IF NOT EXISTS (SELECT 1 FROM category WHERE name = 'Health')
    INSERT INTO category (name, description) VALUES ('Health', 'Health and medical campaigns');

IF NOT EXISTS (SELECT 1 FROM category WHERE name = 'Education')
    INSERT INTO category (name, description) VALUES ('Education', 'Educational campaigns');

IF NOT EXISTS (SELECT 1 FROM category WHERE name = 'Environment')
    INSERT INTO category (name, description) VALUES ('Environment', 'Environmental campaigns');

IF NOT EXISTS (SELECT 1 FROM category WHERE name = 'Arts')
    INSERT INTO category (name, description) VALUES ('Arts', 'Arts and culture campaigns');

IF NOT EXISTS (SELECT 1 FROM category WHERE name = 'Sports')
    INSERT INTO category (name, description) VALUES ('Sports', 'Sports and recreation campaigns');
