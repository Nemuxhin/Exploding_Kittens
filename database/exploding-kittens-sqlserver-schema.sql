USE [Exploding_Kittens];
GO

IF DB_NAME() = N'master'
BEGIN
    THROW 50000, 'This schema script must run in the Exploding_Kittens application database, not master.', 1;
END;
GO

IF OBJECT_ID(N'dbo.user_profile_assignments', N'U') IS NOT NULL DROP TABLE dbo.user_profile_assignments;
IF OBJECT_ID(N'dbo.metadata_template_profile_assignments', N'U') IS NOT NULL DROP TABLE dbo.metadata_template_profile_assignments;
IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NOT NULL DROP TABLE dbo.audit_logs;
IF OBJECT_ID(N'dbo.metadata_review_records', N'U') IS NOT NULL DROP TABLE dbo.metadata_review_records;
IF OBJECT_ID(N'dbo.metadata_fields', N'U') IS NOT NULL DROP TABLE dbo.metadata_fields;
IF OBJECT_ID(N'dbo.metadata_templates', N'U') IS NOT NULL DROP TABLE dbo.metadata_templates;
IF OBJECT_ID(N'dbo.scan_profiles', N'U') IS NOT NULL DROP TABLE dbo.scan_profiles;
IF OBJECT_ID(N'dbo.session_failures', N'U') IS NOT NULL DROP TABLE dbo.session_failures;
IF OBJECT_ID(N'dbo.scan_session_documents', N'U') IS NOT NULL DROP TABLE dbo.scan_session_documents;
IF OBJECT_ID(N'dbo.scan_sessions', N'U') IS NOT NULL DROP TABLE dbo.scan_sessions;
IF OBJECT_ID(N'dbo.document_pages', N'U') IS NOT NULL DROP TABLE dbo.document_pages;
IF OBJECT_ID(N'dbo.documents', N'U') IS NOT NULL DROP TABLE dbo.documents;
IF OBJECT_ID(N'dbo.case_files', N'U') IS NOT NULL DROP TABLE dbo.case_files;
IF OBJECT_ID(N'dbo.boxes', N'U') IS NOT NULL DROP TABLE dbo.boxes;
IF OBJECT_ID(N'dbo.archives', N'U') IS NOT NULL DROP TABLE dbo.archives;
IF OBJECT_ID(N'dbo.clients', N'U') IS NOT NULL DROP TABLE dbo.clients;
IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID(N'dbo.roles', N'U') IS NOT NULL DROP TABLE dbo.roles;
GO

CREATE TABLE dbo.roles (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    username NVARCHAR(100) NOT NULL UNIQUE,
    email NVARCHAR(255) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NULL,
    role_id INT NOT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE',
    is_current_user BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_users_roles FOREIGN KEY (role_id) REFERENCES dbo.roles(id),
    CONSTRAINT CK_users_status CHECK (status IN (N'ACTIVE', N'INACTIVE'))
);

CREATE TABLE dbo.clients (
    id VARCHAR(36) PRIMARY KEY,
    client_number NVARCHAR(100) NOT NULL UNIQUE,
    name NVARCHAR(255) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.archives (
    id VARCHAR(36) PRIMARY KEY,
    client_id VARCHAR(36) NOT NULL,
    archive_code NVARCHAR(100) NOT NULL UNIQUE,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_archives_clients FOREIGN KEY (client_id) REFERENCES dbo.clients(id)
);

CREATE TABLE dbo.boxes (
    id VARCHAR(36) PRIMARY KEY,
    archive_id VARCHAR(36) NULL,
    box_id NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(255) NOT NULL,
    location NVARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_boxes_archives FOREIGN KEY (archive_id) REFERENCES dbo.archives(id)
);

CREATE TABLE dbo.case_files (
    id VARCHAR(36) PRIMARY KEY,
    case_reference NVARCHAR(100) NOT NULL UNIQUE,
    client_id VARCHAR(36) NOT NULL,
    box_id VARCHAR(36) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_case_files_clients FOREIGN KEY (client_id) REFERENCES dbo.clients(id),
    CONSTRAINT FK_case_files_boxes FOREIGN KEY (box_id) REFERENCES dbo.boxes(id)
);

CREATE TABLE dbo.documents (
    id VARCHAR(36) PRIMARY KEY,
    source_item_id NVARCHAR(100) NOT NULL UNIQUE,
    case_file_id VARCHAR(36) NOT NULL,
    title NVARCHAR(255) NULL,
    status NVARCHAR(50) NOT NULL DEFAULT N'IMPORTED',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_documents_case_files FOREIGN KEY (case_file_id) REFERENCES dbo.case_files(id),
    CONSTRAINT CK_documents_status CHECK (status IN (N'IMPORTED', N'METADATA_PENDING', N'QA_PENDING', N'APPROVED', N'REJECTED', N'ARCHIVED'))
);

CREATE TABLE dbo.document_pages (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    page_number INT NOT NULL,
    page_type NVARCHAR(20) NOT NULL,
    source_reference NVARCHAR(500) NOT NULL,
    rotation_degrees INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_document_pages_documents FOREIGN KEY (document_id) REFERENCES dbo.documents(id),
    CONSTRAINT UQ_document_pages_document_page UNIQUE (document_id, page_number),
    CONSTRAINT CK_document_pages_type CHECK (page_type IN (N'TIFF', N'BARCODE')),
    CONSTRAINT CK_document_pages_rotation CHECK (rotation_degrees IN (0, 90, 180, 270))
);

CREATE TABLE dbo.scan_sessions (
    id VARCHAR(36) PRIMARY KEY,
    started_at DATETIME2 NOT NULL,
    box_id VARCHAR(36) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_scan_sessions_boxes FOREIGN KEY (box_id) REFERENCES dbo.boxes(id)
);

CREATE TABLE dbo.scan_session_documents (
    session_id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (session_id, document_id),
    CONSTRAINT FK_scan_session_documents_sessions FOREIGN KEY (session_id) REFERENCES dbo.scan_sessions(id),
    CONSTRAINT FK_scan_session_documents_documents FOREIGN KEY (document_id) REFERENCES dbo.documents(id)
);

CREATE TABLE dbo.session_failures (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    message NVARCHAR(500) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_session_failures_sessions FOREIGN KEY (session_id) REFERENCES dbo.scan_sessions(id)
);

CREATE TABLE dbo.scan_profiles (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    code NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(1000) NULL,
    status NVARCHAR(50) NOT NULL DEFAULT N'Active',
    metadata_template_name NVARCHAR(255) NULL,
    export_naming NVARCHAR(255) NULL,
    last_updated NVARCHAR(255) NULL,
    archived BIT NOT NULL DEFAULT 0,
    barcode_splitting BIT NOT NULL DEFAULT 0,
    barcode_detected_behavior NVARCHAR(100) NULL,
    barcode_page_behavior NVARCHAR(100) NULL,
    default_rotation NVARCHAR(50) NULL,
    brightness NVARCHAR(50) NULL,
    contrast NVARCHAR(50) NULL,
    deskew BIT NOT NULL DEFAULT 0,
    export_format NVARCHAR(50) NULL,
    metadata_required_before_export BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.metadata_templates (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE,
    description NVARCHAR(1000) NULL,
    status NVARCHAR(50) NOT NULL DEFAULT N'Active',
    last_updated NVARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.metadata_fields (
    id INT IDENTITY(1,1) PRIMARY KEY,
    template_id INT NOT NULL,
    name NVARCHAR(100) NOT NULL,
    type NVARCHAR(50) NOT NULL,
    required BIT NOT NULL DEFAULT 0,
    placeholder NVARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_metadata_fields_templates FOREIGN KEY (template_id) REFERENCES dbo.metadata_templates(id),
    CONSTRAINT UQ_metadata_fields_template_name UNIQUE (template_id, name)
);

CREATE TABLE dbo.metadata_template_profile_assignments (
    metadata_template_id INT NOT NULL,
    scan_profile_id INT NOT NULL,
    PRIMARY KEY (metadata_template_id, scan_profile_id),
    CONSTRAINT FK_metadata_template_profile_assignments_templates FOREIGN KEY (metadata_template_id) REFERENCES dbo.metadata_templates(id),
    CONSTRAINT FK_metadata_template_profile_assignments_profiles FOREIGN KEY (scan_profile_id) REFERENCES dbo.scan_profiles(id)
);

CREATE TABLE dbo.user_profile_assignments (
    user_id INT NOT NULL,
    scan_profile_id INT NOT NULL,
    PRIMARY KEY (user_id, scan_profile_id),
    CONSTRAINT FK_user_profile_assignments_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_user_profile_assignments_profiles FOREIGN KEY (scan_profile_id) REFERENCES dbo.scan_profiles(id)
);

CREATE TABLE dbo.metadata_review_records (
    id NVARCHAR(100) PRIMARY KEY,
    identity_value NVARCHAR(255) NOT NULL,
    client_name NVARCHAR(255) NULL,
    archive_name NVARCHAR(255) NULL,
    profile_name NVARCHAR(255) NULL,
    metadata_template_name NVARCHAR(255) NULL,
    metadata_status NVARCHAR(100) NULL,
    qa_status NVARCHAR(100) NULL,
    pages INT NOT NULL DEFAULT 0,
    last_updated NVARCHAR(255) NULL,
    assigned_to NVARCHAR(255) NULL,
    scanned_by NVARCHAR(255) NULL,
    date_group NVARCHAR(100) NULL,
    warning BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.audit_logs (
    id INT IDENTITY(1,1) PRIMARY KEY,
    timestamp DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    type NVARCHAR(100) NOT NULL,
    actor NVARCHAR(255) NOT NULL,
    action NVARCHAR(255) NOT NULL,
    target NVARCHAR(255) NOT NULL,
    status NVARCHAR(100) NOT NULL,
    description NVARCHAR(1000) NULL
);
GO

CREATE INDEX IX_archives_client_id ON dbo.archives(client_id);
CREATE INDEX IX_boxes_archive_id ON dbo.boxes(archive_id);
CREATE INDEX IX_case_files_client_id ON dbo.case_files(client_id);
CREATE INDEX IX_case_files_box_id ON dbo.case_files(box_id);
CREATE INDEX IX_documents_case_file_id ON dbo.documents(case_file_id);
CREATE INDEX IX_document_pages_document_id ON dbo.document_pages(document_id);
CREATE INDEX IX_scan_sessions_box_id ON dbo.scan_sessions(box_id);
CREATE INDEX IX_session_failures_session_id ON dbo.session_failures(session_id);
CREATE INDEX IX_metadata_fields_template_id ON dbo.metadata_fields(template_id);
CREATE INDEX IX_metadata_review_records_identity_value ON dbo.metadata_review_records(identity_value);
CREATE INDEX IX_audit_logs_timestamp ON dbo.audit_logs(timestamp);
GO

INSERT INTO dbo.roles (name, description)
VALUES
    (N'ADMIN', N'Can manage users, profiles and metadata configuration'),
    (N'SCANNER', N'Can scan and organize imported TIFF items'),
    (N'QA', N'Can quality assure metadata and scanned documents'),
    (N'USER', N'Basic application user');

INSERT INTO dbo.users (name, username, email, password_hash, role_id, status, is_current_user)
VALUES (
    N'Exploding_Kittens Admin',
    N'admin',
    N'admin@exploding-kittens.local',
    N'240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    (SELECT id FROM dbo.roles WHERE name = N'ADMIN'),
    N'ACTIVE',
    1
);

INSERT INTO dbo.scan_profiles (
    name, code, description, status, metadata_template_name, export_naming, last_updated,
    archived, barcode_splitting, barcode_detected_behavior, barcode_page_behavior,
    default_rotation, brightness, contrast, deskew, export_format, metadata_required_before_export
)
VALUES
    (N'Standard Intake', N'PROFILE-STANDARD', N'Default scanning profile for general intake.',
     N'Active', N'Standard Metadata', N'{clientNumber}_{caseReference}_{sourceItemId}', N'Created from schema',
     0, 1, N'Split Document', N'Keep Barcode Page', N'0', N'Normal', N'Normal', 1, N'TIFF', 1),
    (N'Drawing Archive', N'PROFILE-DRAWING', N'Profile tuned for drawing and oversized material.',
     N'Active', N'Drawing Metadata', N'{clientNumber}_{boxId}_{sourceItemId}', N'Created from schema',
     0, 0, N'Flag', N'Separate Barcode Page', N'90', N'Brighten', N'Normal', 1, N'PDF', 0);

INSERT INTO dbo.metadata_templates (name, description, status, last_updated)
VALUES
    (N'Standard Metadata', N'Default metadata set for regular intake.', N'Active', N'Created from schema'),
    (N'Drawing Metadata', N'Metadata set for drawings and large-format scans.', N'Active', N'Created from schema');

DECLARE @standardTemplateId INT = (SELECT id FROM dbo.metadata_templates WHERE name = N'Standard Metadata');
DECLARE @drawingTemplateId INT = (SELECT id FROM dbo.metadata_templates WHERE name = N'Drawing Metadata');
DECLARE @standardProfileId INT = (SELECT id FROM dbo.scan_profiles WHERE code = N'PROFILE-STANDARD');
DECLARE @drawingProfileId INT = (SELECT id FROM dbo.scan_profiles WHERE code = N'PROFILE-DRAWING');
DECLARE @adminUserId INT = (SELECT id FROM dbo.users WHERE username = N'admin');

INSERT INTO dbo.metadata_fields (template_id, name, type, required, placeholder, sort_order)
VALUES
    (@standardTemplateId, N'caseOwner', N'Text', 1, N'Enter case owner', 1),
    (@standardTemplateId, N'documentDate', N'Date', 0, N'Select date', 2),
    (@standardTemplateId, N'priority', N'Dropdown', 0, N'Select priority', 3),
    (@drawingTemplateId, N'drawingNumber', N'Text', 1, N'Enter drawing number', 1),
    (@drawingTemplateId, N'revision', N'Text', 0, N'Enter revision', 2);

INSERT INTO dbo.metadata_template_profile_assignments (metadata_template_id, scan_profile_id)
VALUES
    (@standardTemplateId, @standardProfileId),
    (@drawingTemplateId, @drawingProfileId);

INSERT INTO dbo.user_profile_assignments (user_id, scan_profile_id)
VALUES
    (@adminUserId, @standardProfileId),
    (@adminUserId, @drawingProfileId);

INSERT INTO dbo.audit_logs (type, actor, action, target, status, description)
VALUES
    (N'System', N'Schema', N'Created database', N'Exploding_Kittens', N'Success', N'Initial schema and seed data applied.');
GO

SELECT N'Exploding_Kittens SQL Server schema created successfully' AS message;
GO
