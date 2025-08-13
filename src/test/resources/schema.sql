-- Database schema creation script for TestContainers
-- This script creates all necessary tables and initial data for integration tests

-- Drop tables if they exist (in reverse dependency order)
IF OBJECT_ID('user_categories', 'U') IS NOT NULL DROP TABLE user_categories;
IF OBJECT_ID('follower', 'U') IS NOT NULL DROP TABLE follower;
IF OBJECT_ID('publication', 'U') IS NOT NULL DROP TABLE publication;
IF OBJECT_ID('goal', 'U') IS NOT NULL DROP TABLE goal;
IF OBJECT_ID('campaign', 'U') IS NOT NULL DROP TABLE campaign;
IF OBJECT_ID('bank_account', 'U') IS NOT NULL DROP TABLE bank_account;
IF OBJECT_ID('verification_request', 'U') IS NOT NULL DROP TABLE verification_request;
IF OBJECT_ID('revoked_token', 'U') IS NOT NULL DROP TABLE revoked_token;
IF OBJECT_ID('user_info', 'U') IS NOT NULL DROP TABLE user_info;
IF OBJECT_ID('[user]', 'U') IS NOT NULL DROP TABLE [user];
IF OBJECT_ID('categories', 'U') IS NOT NULL DROP TABLE categories;
IF OBJECT_ID('roles', 'U') IS NOT NULL DROP TABLE roles;

-- Create roles table
CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Create categories table
CREATE TABLE categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Create user table (using [user] because 'user' is a reserved keyword in SQL Server)
CREATE TABLE [user] (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    role_id BIGINT NOT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    is_verified BIT NOT NULL DEFAULT 0,
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Create user_info table
CREATE TABLE user_info (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    user_name VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    description TEXT,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE
);

-- Create user_categories table (many-to-many relationship)
CREATE TABLE user_categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    UNIQUE(user_id, category_id)
);

-- Create verification_request table
CREATE TABLE verification_request (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    document_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'PENDING',
    request_date DATETIME2 DEFAULT GETDATE(),
    approved_date DATETIME2 NULL,
    notes TEXT,
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE
);

-- Create bank_account table
CREATE TABLE bank_account (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE
);

-- Create campaign table
CREATE TABLE campaign (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE
);

-- Create goal table
CREATE TABLE goal (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    campaign_id BIGINT NOT NULL UNIQUE,
    amount_goal DECIMAL(15,2) NOT NULL DEFAULT 0,
    amount_raised DECIMAL(15,2) NOT NULL DEFAULT 0,
    target_date DATETIME2,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE CASCADE
);

-- Create publication table
CREATE TABLE publication (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    campaign_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    publication_date DATETIME2 DEFAULT GETDATE(),
    deleted_at DATETIME2 NULL,
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE NO ACTION
);

-- Create follower table (user following relationship)
CREATE TABLE follower (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    follower_user_id BIGINT NOT NULL,
    followed_user_id BIGINT NOT NULL,
    follow_date DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (follower_user_id) REFERENCES [user](id) ON DELETE CASCADE,
    FOREIGN KEY (followed_user_id) REFERENCES [user](id) ON DELETE NO ACTION,
    UNIQUE(follower_user_id, followed_user_id),
    CHECK (follower_user_id != followed_user_id)
);

-- Create revoked_token table for JWT token blacklist
CREATE TABLE revoked_token (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    token_jti VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    revoked_at DATETIME2 DEFAULT GETDATE(),
    expiry_date DATETIME2 NOT NULL,
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE
);

-- Insert initial roles
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

-- Insert initial categories for testing
INSERT INTO categories (name, description) VALUES 
    ('Technology', 'Technology related campaigns'),
    ('Health', 'Health and medical campaigns'),
    ('Education', 'Educational campaigns'),
    ('Environment', 'Environmental campaigns'),
    ('Arts', 'Arts and culture campaigns'),
    ('Sports', 'Sports and recreation campaigns');

-- Create indexes for better performance
CREATE INDEX idx_user_email ON user_info(email);
CREATE INDEX idx_user_username ON user_info(user_name);
CREATE INDEX idx_user_active ON [user](is_active);
CREATE INDEX idx_user_verified ON [user](is_verified);
CREATE INDEX idx_user_deleted ON [user](deleted_at);
CREATE INDEX idx_campaign_user ON campaign(user_id);
CREATE INDEX idx_campaign_status ON campaign(status);
CREATE INDEX idx_publication_user ON publication(user_id);
CREATE INDEX idx_publication_campaign ON publication(campaign_id);
CREATE INDEX idx_follower_follower_user ON follower(follower_user_id);
CREATE INDEX idx_follower_followed_user ON follower(followed_user_id);
CREATE INDEX idx_revoked_token_jti ON revoked_token(token_jti);
CREATE INDEX idx_revoked_token_user ON revoked_token(user_id);
CREATE INDEX idx_revoked_token_expiry ON revoked_token(expiry_date);
