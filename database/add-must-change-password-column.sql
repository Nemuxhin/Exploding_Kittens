-- Adds the users.must_change_password column used by the login / force-password-change flow.
--
-- This replaces the old runtime "ALTER TABLE" that UserDAO executed on construction.
-- Schema changes belong in versioned scripts (run once by an examiner/DBA), never in
-- application code. Safe to run repeatedly: it only adds the column if it is missing.

USE [Exploding_KittensV2];
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.users')
      AND name = N'must_change_password'
)
BEGIN
    ALTER TABLE dbo.users
        ADD must_change_password BIT NOT NULL
        CONSTRAINT DF_users_must_change_password DEFAULT 0;
END
GO
