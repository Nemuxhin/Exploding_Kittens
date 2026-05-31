USE [Exploding_KittensV2];
GO

UPDATE dbo.users
SET password_hash = N'240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    updated_at = SYSUTCDATETIME()
WHERE username = N'admin';
GO

SELECT username, password_hash
FROM dbo.users
WHERE username = N'admin';
GO
