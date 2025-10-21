-- =====================================================
-- Migration: Add User Moderation Fields and Campaign Soft Delete
-- Description:
--   1. Add moderation fields to user table (banned, suspended_until, etc.)
--   2. Add soft delete field to campaign table
-- Date: 2025-10-21
-- =====================================================

USE ApiVaPaTiJava;
GO

-- =====================================================
-- 1. Add moderation fields to [user] table
-- =====================================================

-- Check if columns exist before adding them
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[user]') AND name = 'banned')
BEGIN
    ALTER TABLE [user]
    ADD banned BIT NULL DEFAULT 0;

    PRINT 'Column banned added to [user] table';
END
ELSE
BEGIN
    PRINT 'Column banned already exists in [user] table';
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[user]') AND name = 'banned_at')
BEGIN
    ALTER TABLE [user]
    ADD banned_at DATETIME2 NULL;

    PRINT 'Column banned_at added to [user] table';
END
ELSE
BEGIN
    PRINT 'Column banned_at already exists in [user] table';
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[user]') AND name = 'suspended_until')
BEGIN
    ALTER TABLE [user]
    ADD suspended_until DATETIME2 NULL;

    PRINT 'Column suspended_until added to [user] table';
END
ELSE
BEGIN
    PRINT 'Column suspended_until already exists in [user] table';
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[user]') AND name = 'banned_reason')
BEGIN
    ALTER TABLE [user]
    ADD banned_reason NVARCHAR(500) NULL;

    PRINT 'Column banned_reason added to [user] table';
END
ELSE
BEGIN
    PRINT 'Column banned_reason already exists in [user] table';
END
GO

-- Update existing users to have banned = 0 (false) by default
UPDATE [user]
SET banned = 0
WHERE banned IS NULL;
GO

PRINT 'Updated existing users to have banned = 0';
GO

-- =====================================================
-- 2. Add soft delete field to campaign table
-- =====================================================

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'campaign') AND name = 'deleted_at')
BEGIN
    ALTER TABLE campaign
    ADD deleted_at DATETIME2 NULL;

    PRINT 'Column deleted_at added to campaign table';
END
ELSE
BEGIN
    PRINT 'Column deleted_at already exists in campaign table';
END
GO

-- =====================================================
-- Verification
-- =====================================================

-- Show new columns in [user] table
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'user'
AND COLUMN_NAME IN ('banned', 'banned_at', 'suspended_until', 'banned_reason')
ORDER BY ORDINAL_POSITION;
GO

-- Show new column in campaign table
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'campaign'
AND COLUMN_NAME = 'deleted_at';
GO

PRINT 'Migration completed successfully!';
GO
