-- Lets report.reported_entity_type accept every ReportedEntityType value (COMMENT is new).
-- Hibernate 6 created this column with a CHECK listing the enum values of that moment, and ddl-auto=update never
-- changes it. Drops any CHECK on the column and recreates it with a fixed name. Idempotent.
-- Run by hand before starting the new image: sqlcmd -b
DECLARE @name sysname;
DECLARE @sql nvarchar(400);

-- 1. Drop the CHECKs on the column, except the one this script creates
SELECT TOP 1 @name = cc.name
FROM sys.check_constraints cc
LEFT JOIN sys.columns c ON c.object_id = cc.parent_object_id AND c.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID('report')
  AND (c.name = 'reported_entity_type' OR cc.definition LIKE '%reported[_]entity[_]type%')
  AND cc.name <> 'CK_report_reported_entity_type';

WHILE @name IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE report DROP CONSTRAINT ' + QUOTENAME(@name);
    EXEC sp_executesql @sql;
    SET @name = NULL;
    SELECT TOP 1 @name = cc.name
    FROM sys.check_constraints cc
    LEFT JOIN sys.columns c ON c.object_id = cc.parent_object_id AND c.column_id = cc.parent_column_id
    WHERE cc.parent_object_id = OBJECT_ID('report')
      AND (c.name = 'reported_entity_type' OR cc.definition LIKE '%reported[_]entity[_]type%')
      AND cc.name <> 'CK_report_reported_entity_type';
END
GO

-- 2. The CHECK with every value, with a fixed name so the next value only has to drop it by name
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = 'CK_report_reported_entity_type' AND parent_object_id = OBJECT_ID('report'))
    ALTER TABLE report ADD CONSTRAINT CK_report_reported_entity_type
        CHECK (reported_entity_type IN ('USER', 'PUBLICATION', 'CAMPAIGN', 'COMMENT'));
GO
