-- Adds donation.recipient_user_id (backfilled with the campaign owner, NOT NULL, FK and index)
-- and makes donation.campaign_id nullable for direct donations.
-- Idempotent. Run with the app stopped, before starting the new image: sqlcmd -I -b
-- 1. New column, nullable so it can be filled
IF COL_LENGTH('donation', 'recipient_user_id') IS NULL
    ALTER TABLE donation ADD recipient_user_id BIGINT NULL;
GO

-- 2. Backfill: the recipient of a campaign donation is the campaign owner (deleted campaigns and owners included:
--    native SQL does not apply @SQLRestriction)
UPDATE d SET d.recipient_user_id = c.user_id
FROM donation d JOIN campaign c ON c.id = d.campaign_id
WHERE d.recipient_user_id IS NULL;
GO

-- 3. No row may be left without a recipient
IF EXISTS (SELECT 1 FROM donation WHERE recipient_user_id IS NULL)
    RAISERROR('donation migration: donations without recipient_user_id', 16, 1);
GO

-- 4. NOT NULL, FK and index
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_NAME = 'donation' AND COLUMN_NAME = 'recipient_user_id' AND IS_NULLABLE = 'YES')
    ALTER TABLE donation ALTER COLUMN recipient_user_id BIGINT NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_donation_recipient_user')
    ALTER TABLE donation ADD CONSTRAINT FK_donation_recipient_user
        FOREIGN KEY (recipient_user_id) REFERENCES [user](id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_donation_recipient' AND object_id = OBJECT_ID('donation'))
    CREATE INDEX idx_donation_recipient ON donation(recipient_user_id);
GO

-- 5. (reserved: drop the FK/index on campaign_id here if step 6 fails, with the names from sp_fkeys / sp_helpindex)

-- 6. campaign_id accepts NULL (direct donations)
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_NAME = 'donation' AND COLUMN_NAME = 'campaign_id' AND IS_NULLABLE = 'NO')
    ALTER TABLE donation ALTER COLUMN campaign_id BIGINT NULL;
GO
