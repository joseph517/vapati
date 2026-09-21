-- Database schema creation script for TestContainers
-- This script creates all necessary tables and initial data for integration tests

-- Drop tables if they exist (in reverse dependency order)
IF OBJECT_ID('donation', 'U') IS NOT NULL DROP TABLE donation;
IF OBJECT_ID('report', 'U') IS NOT NULL DROP TABLE report;
IF OBJECT_ID('campaign_category', 'U') IS NOT NULL DROP TABLE campaign_category;
IF OBJECT_ID('user_category', 'U') IS NOT NULL DROP TABLE user_category;
IF OBJECT_ID('followers', 'U') IS NOT NULL DROP TABLE followers;
IF OBJECT_ID('publication', 'U') IS NOT NULL DROP TABLE publication;
IF OBJECT_ID('campaign', 'U') IS NOT NULL DROP TABLE campaign;
IF OBJECT_ID('goal', 'U') IS NOT NULL DROP TABLE goal;
IF OBJECT_ID('bank_accounts', 'U') IS NOT NULL DROP TABLE bank_accounts;
IF OBJECT_ID('verification_request', 'U') IS NOT NULL DROP TABLE verification_request;
IF OBJECT_ID('revoked_tokens', 'U') IS NOT NULL DROP TABLE revoked_tokens;
IF OBJECT_ID('user_info', 'U') IS NOT NULL DROP TABLE user_info;
IF OBJECT_ID('[user]', 'U') IS NOT NULL DROP TABLE [user];
IF OBJECT_ID('category', 'U') IS NOT NULL DROP TABLE category;
IF OBJECT_ID('roles', 'U') IS NOT NULL DROP TABLE roles;

-- Create roles table
CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Create category table
CREATE TABLE category (
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
    banned BIT DEFAULT 0,
    banned_at DATETIME2 NULL,
    suspended_until DATETIME2 NULL,
    banned_reason VARCHAR(500) NULL,
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
    phone VARCHAR(20) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    profile_picture VARCHAR(500) NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE
);

-- Create user_category table (many-to-many relationship)
CREATE TABLE user_category (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category(id),
    UNIQUE(user_id, category_id)
);

-- Create verification_request table
CREATE TABLE verification_request (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    dni_front VARCHAR(500) NOT NULL,
    dni_back VARCHAR(500) NOT NULL,
    selfie_user VARCHAR(500) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    deleted_at DATETIME2 NULL,
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE
);

-- Create bank_accounts table
CREATE TABLE bank_accounts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    account_type VARCHAR(50) NOT NULL,
    is_verified BIT NOT NULL DEFAULT 0,
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE
);

-- Create goal table
CREATE TABLE goal (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    amount_goal DECIMAL(15,2) NOT NULL DEFAULT 0,
    amount_raised DECIMAL(15,2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    target_date DATETIME2,
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Create campaign table
CREATE TABLE campaign (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goal_id BIGINT UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE,
    FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE
);

-- Create campaign_category table (many-to-many relationship)
CREATE TABLE campaign_category (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category(id),
    UNIQUE(campaign_id, category_id)
);

-- Create publication table
CREATE TABLE publication (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    campaign_id BIGINT NULL,
    description VARCHAR(MAX),
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE NO ACTION
);

-- Create followers table (user following relationship)
CREATE TABLE followers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    follower_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES [user](id) ON DELETE CASCADE,
    FOREIGN KEY (follower_id) REFERENCES [user](id) ON DELETE NO ACTION,
    UNIQUE(user_id, follower_id),
    CHECK (user_id != follower_id)
);

-- Create donation table
CREATE TABLE donation (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    donor_user_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(100) UNIQUE,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (donor_user_id) REFERENCES [user](id) ON DELETE CASCADE,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE NO ACTION,
    CHECK (amount > 0)
);

-- Create report table (content/user moderation reports)
CREATE TABLE report (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    reported_entity_type VARCHAR(50) NOT NULL,
    reported_entity_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME2 NULL,
    admin_notes TEXT,
    action_taken VARCHAR(50) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NULL,
    FOREIGN KEY (reporter_id) REFERENCES [user](id) ON DELETE NO ACTION,
    FOREIGN KEY (reviewed_by) REFERENCES [user](id) ON DELETE NO ACTION,
    UNIQUE(reporter_id, reported_entity_type, reported_entity_id)
);

-- Create revoked_tokens table for JWT token blacklist
CREATE TABLE revoked_tokens (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    expiration_date DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

-- Insert initial roles
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

-- Insert initial categories for testing
INSERT INTO category (name, description) VALUES
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
CREATE INDEX idx_campaign_goal ON campaign(goal_id);
CREATE INDEX idx_campaign_status ON campaign(status);
CREATE INDEX idx_campaign_deleted ON campaign(deleted_at);
CREATE INDEX idx_goal_status ON goal(status);
CREATE INDEX idx_goal_deleted ON goal(deleted_at);
CREATE INDEX idx_donation_donor ON donation(donor_user_id);
CREATE INDEX idx_donation_campaign ON donation(campaign_id);
CREATE INDEX idx_donation_status ON donation(status);
CREATE INDEX idx_donation_created ON donation(created_at);
CREATE INDEX idx_publication_user ON publication(user_id);
CREATE INDEX idx_publication_campaign ON publication(campaign_id);
CREATE INDEX idx_followers_user ON followers(user_id);
CREATE INDEX idx_followers_follower ON followers(follower_id);
CREATE INDEX idx_revoked_tokens_token ON revoked_tokens(token);
CREATE INDEX idx_revoked_tokens_expiration ON revoked_tokens(expiration_date);
CREATE INDEX idx_report_reporter ON report(reporter_id);
CREATE INDEX idx_report_status ON report(status);
