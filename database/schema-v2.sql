/*
    Exploding_Kittens schema v2

    Main changes from the current mixed schema:
    - removes dbo.case_files
    - removes dbo.session_failures
    - stores case ownership directly on dbo.documents
    - stores the latest failure state directly on dbo.scan_sessions
    - drops legacy duplicate tables as part of the reset

    This script is intended for a disposable / reset database.
    It drops known tables first, then recreates the v2 schema.
*/

SET NOCOUNT ON;
SET XACT_ABORT ON;

/* ------------------------------------------------------------------------- */
/* Drop existing tables                                                      */
/* ------------------------------------------------------------------------- */

DROP TABLE IF EXISTS dbo.user_notifications;
DROP TABLE IF EXISTS dbo.qa_review_pages;
DROP TABLE IF EXISTS dbo.qa_reviews;
DROP TABLE IF EXISTS dbo.scan_saved_progress_pages;
DROP TABLE IF EXISTS dbo.scan_saved_progress;
DROP TABLE IF EXISTS dbo.scan_session_documents;
DROP TABLE IF EXISTS dbo.document_pages;
DROP TABLE IF EXISTS dbo.documents;
DROP TABLE IF EXISTS dbo.user_profile_assignments;
DROP TABLE IF EXISTS dbo.metadata_template_profile_assignments;
DROP TABLE IF EXISTS dbo.metadata_template_profiles;
DROP TABLE IF EXISTS dbo.metadata_fields;
DROP TABLE IF EXISTS dbo.metadata_templates;
DROP TABLE IF EXISTS dbo.metadata_review_records;
DROP TABLE IF EXISTS dbo.scan_sessions;
DROP TABLE IF EXISTS dbo.session_failures;
DROP TABLE IF EXISTS dbo.case_files;
DROP TABLE IF EXISTS dbo.scan_profiles;
DROP TABLE IF EXISTS dbo.user_profiles;
DROP TABLE IF EXISTS dbo.users;
DROP TABLE IF EXISTS dbo.roles;
DROP TABLE IF EXISTS dbo.profiles;
DROP TABLE IF EXISTS dbo.boxes;
DROP TABLE IF EXISTS dbo.archives;
DROP TABLE IF EXISTS dbo.clients;

/* ------------------------------------------------------------------------- */
/* Core access tables                                                        */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.roles
(
    id INT IDENTITY(1,1) NOT NULL
        CONSTRAINT PK_roles PRIMARY KEY,
    name NVARCHAR(50) NOT NULL
        CONSTRAINT UQ_roles_name UNIQUE,
    description NVARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_roles_created_at DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.users
(
    id INT IDENTITY(1,1) NOT NULL
        CONSTRAINT PK_users PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    username NVARCHAR(100) NOT NULL
        CONSTRAINT UQ_users_username UNIQUE,
    email NVARCHAR(255) NOT NULL
        CONSTRAINT UQ_users_email UNIQUE,
    password_hash NVARCHAR(255) NULL,
    role_id INT NOT NULL,
    status NVARCHAR(50) NOT NULL
        CONSTRAINT DF_users_status DEFAULT N'ACTIVE',
    is_current_user BIT NOT NULL
        CONSTRAINT DF_users_is_current_user DEFAULT 0,
    must_change_password BIT NOT NULL
        CONSTRAINT DF_users_must_change_password DEFAULT 0,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_users_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_users_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_users_roles FOREIGN KEY (role_id) REFERENCES dbo.roles(id),
    CONSTRAINT CK_users_status CHECK (status IN (N'ACTIVE', N'INACTIVE'))
);

INSERT INTO dbo.roles (name, description)
VALUES
    (N'Admin', N'Administrative access'),
    (N'User', N'Standard scanning access'),
    (N'QA', N'Quality assurance access');

/* ------------------------------------------------------------------------- */
/* Domain lookup tables                                                      */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.clients
(
    id VARCHAR(36) NOT NULL
        CONSTRAINT PK_clients PRIMARY KEY,
    client_number NVARCHAR(100) NOT NULL
        CONSTRAINT UQ_clients_client_number UNIQUE,
    name NVARCHAR(255) NOT NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_clients_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_clients_updated_at DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.boxes
(
    id VARCHAR(36) NOT NULL
        CONSTRAINT PK_boxes PRIMARY KEY,
    box_id NVARCHAR(100) NOT NULL
        CONSTRAINT UQ_boxes_box_id UNIQUE,
    description NVARCHAR(255) NOT NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_boxes_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_boxes_updated_at DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.scan_profiles
(
    id INT IDENTITY(1,1) NOT NULL
        CONSTRAINT PK_scan_profiles PRIMARY KEY,
    name NVARCHAR(255) NOT NULL
        CONSTRAINT UQ_scan_profiles_name UNIQUE,
    code NVARCHAR(100) NOT NULL
        CONSTRAINT UQ_scan_profiles_code UNIQUE,
    description NVARCHAR(1000) NULL,
    status NVARCHAR(50) NOT NULL
        CONSTRAINT DF_scan_profiles_status DEFAULT N'Active',
    metadata_template_name NVARCHAR(255) NULL,
    export_naming NVARCHAR(255) NULL,
    last_updated NVARCHAR(255) NULL,
    archived BIT NOT NULL
        CONSTRAINT DF_scan_profiles_archived DEFAULT 0,
    barcode_splitting BIT NOT NULL
        CONSTRAINT DF_scan_profiles_barcode_splitting DEFAULT 0,
    barcode_detected_behavior NVARCHAR(100) NULL,
    barcode_page_behavior NVARCHAR(100) NULL,
    default_rotation NVARCHAR(50) NULL,
    brightness NVARCHAR(50) NULL,
    contrast NVARCHAR(50) NULL,
    deskew BIT NOT NULL
        CONSTRAINT DF_scan_profiles_deskew DEFAULT 0,
    export_format NVARCHAR(50) NULL,
    metadata_required_before_export BIT NOT NULL
        CONSTRAINT DF_scan_profiles_metadata_required_before_export DEFAULT 0,
    client_name NVARCHAR(255) NULL,
    client VARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_scan_profiles_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_scan_profiles_updated_at DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.user_profile_assignments
(
    user_id INT NOT NULL,
    scan_profile_id INT NOT NULL,
    CONSTRAINT PK_user_profile_assignments PRIMARY KEY (user_id, scan_profile_id),
    CONSTRAINT FK_user_profile_assignments_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_profile_assignments_profile
        FOREIGN KEY (scan_profile_id) REFERENCES dbo.scan_profiles(id) ON DELETE CASCADE
);

/* ------------------------------------------------------------------------- */
/* Audit and metadata review tables                                          */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.audit_logs
(
    id INT IDENTITY(1,1) NOT NULL
        CONSTRAINT PK_audit_logs PRIMARY KEY,
    timestamp DATETIME2 NOT NULL
        CONSTRAINT DF_audit_logs_timestamp DEFAULT SYSUTCDATETIME(),
    type NVARCHAR(100) NOT NULL,
    actor NVARCHAR(255) NOT NULL,
    action NVARCHAR(255) NOT NULL,
    target NVARCHAR(500) NOT NULL,
    status NVARCHAR(100) NOT NULL,
    export_id UNIQUEIDENTIFIER NULL,
    description NVARCHAR(1000) NOT NULL
);

CREATE TABLE dbo.metadata_review_records
(
    id VARCHAR(255) NOT NULL
        CONSTRAINT PK_metadata_review_records PRIMARY KEY,
    identity_value NVARCHAR(255) NOT NULL,
    client_name NVARCHAR(255) NOT NULL,
    archive_name NVARCHAR(255) NOT NULL,
    profile_name NVARCHAR(255) NOT NULL,
    metadata_template_name NVARCHAR(255) NOT NULL,
    metadata_status NVARCHAR(100) NOT NULL,
    qa_status NVARCHAR(100) NOT NULL,
    pages INT NOT NULL,
    last_updated NVARCHAR(255) NOT NULL,
    assigned_to NVARCHAR(255) NOT NULL,
    scanned_by NVARCHAR(255) NOT NULL,
    date_group NVARCHAR(255) NOT NULL,
    warning BIT NOT NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_metadata_review_records_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_metadata_review_records_updated_at DEFAULT SYSUTCDATETIME()
);

CREATE INDEX IX_metadata_review_records_updated_at
    ON dbo.metadata_review_records(updated_at DESC, created_at DESC);

/* ------------------------------------------------------------------------- */
/* Document storage                                                          */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.documents
(
    id VARCHAR(36) NOT NULL
        CONSTRAINT PK_documents PRIMARY KEY,
    source_item_id NVARCHAR(255) NOT NULL
        CONSTRAINT UQ_documents_source_item_id UNIQUE,
    case_reference NVARCHAR(255) NOT NULL,
    client_id VARCHAR(36) NOT NULL,
    box_id VARCHAR(36) NOT NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_documents_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_documents_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_documents_clients FOREIGN KEY (client_id) REFERENCES dbo.clients(id),
    CONSTRAINT FK_documents_boxes FOREIGN KEY (box_id) REFERENCES dbo.boxes(id)
);

CREATE INDEX IX_documents_client_id ON dbo.documents(client_id);
CREATE INDEX IX_documents_box_id ON dbo.documents(box_id);
CREATE INDEX IX_documents_case_reference ON dbo.documents(case_reference);

CREATE TABLE dbo.document_pages
(
    id VARCHAR(36) NOT NULL
        CONSTRAINT PK_document_pages PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    page_number INT NOT NULL,
    page_order INT NOT NULL
        CONSTRAINT DF_document_pages_page_order DEFAULT 1,
    page_type NVARCHAR(20) NOT NULL,
    source_reference NVARCHAR(500) NOT NULL,
    reference_id INT NOT NULL
        CONSTRAINT DF_document_pages_reference_id DEFAULT 0,
    rotation_degrees INT NOT NULL
        CONSTRAINT DF_document_pages_rotation_degrees DEFAULT 0,
    display_content VARCHAR(MAX) NOT NULL
        CONSTRAINT DF_document_pages_display_content DEFAULT '',
    deleted_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_document_pages_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_document_pages_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_document_pages_documents
        FOREIGN KEY (document_id) REFERENCES dbo.documents(id) ON DELETE CASCADE,
    CONSTRAINT UQ_document_pages_document_page UNIQUE (document_id, page_number),
    CONSTRAINT CK_document_pages_type CHECK (page_type IN (N'BARCODE', N'TIFF')),
    CONSTRAINT CK_document_pages_rotation CHECK (rotation_degrees IN (0, 90, 180, 270))
);

CREATE INDEX IX_document_pages_document_id
    ON dbo.document_pages(document_id, page_order, id);

/* ------------------------------------------------------------------------- */
/* Scan sessions                                                             */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.scan_sessions
(
    id VARCHAR(36) NOT NULL
        CONSTRAINT PK_scan_sessions PRIMARY KEY,
    started_at DATETIME2 NOT NULL,
    box_id VARCHAR(36) NOT NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_scan_sessions_created_at DEFAULT SYSUTCDATETIME(),
    selected_barcode_behavior VARCHAR(255) NOT NULL
        CONSTRAINT DF_scan_sessions_selected_barcode_behavior DEFAULT '',
    last_status VARCHAR(50) NOT NULL
        CONSTRAINT DF_scan_sessions_last_status DEFAULT 'READY',
    profile_name VARCHAR(255) NULL,
    created_by_user_id INT NULL,
    last_failure_message NVARCHAR(1000) NULL,
    last_failure_at DATETIME2 NULL,
    failure_count INT NOT NULL
        CONSTRAINT DF_scan_sessions_failure_count DEFAULT 0,
    CONSTRAINT FK_scan_sessions_boxes FOREIGN KEY (box_id) REFERENCES dbo.boxes(id),
    CONSTRAINT FK_scan_sessions_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(id)
);

CREATE INDEX IX_scan_sessions_box_id
    ON dbo.scan_sessions(box_id);

CREATE INDEX IX_scan_sessions_created_by_started_at
    ON dbo.scan_sessions(created_by_user_id, started_at DESC);

CREATE TABLE dbo.scan_session_documents
(
    session_id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    CONSTRAINT PK_scan_session_documents PRIMARY KEY (session_id, document_id),
    CONSTRAINT FK_scan_session_documents_session
        FOREIGN KEY (session_id) REFERENCES dbo.scan_sessions(id) ON DELETE CASCADE,
    CONSTRAINT FK_scan_session_documents_document
        FOREIGN KEY (document_id) REFERENCES dbo.documents(id) ON DELETE CASCADE
);

/* ------------------------------------------------------------------------- */
/* Exports                                                                   */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.exports
(
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT PK_exports PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NULL,
    exported_by_user_id INT NULL,
    export_format VARCHAR(50) NOT NULL,
    export_status VARCHAR(50) NOT NULL,
    file_name NVARCHAR(255) NOT NULL,
    file_path NVARCHAR(1024) NOT NULL,
    exported_at DATETIME2 NOT NULL,
    error_message NVARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_exports_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_exports_session FOREIGN KEY (session_id) REFERENCES dbo.scan_sessions(id),
    CONSTRAINT FK_exports_document FOREIGN KEY (document_id) REFERENCES dbo.documents(id),
    CONSTRAINT FK_exports_user FOREIGN KEY (exported_by_user_id) REFERENCES dbo.users(id),
    CONSTRAINT CK_exports_status CHECK (export_status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT CK_exports_format CHECK (export_format IN ('TIFF', 'PDF', 'ZIP'))
);

CREATE INDEX IX_exports_session_exported_at
    ON dbo.exports(session_id, exported_at DESC);

CREATE INDEX IX_exports_document_exported_at
    ON dbo.exports(document_id, exported_at DESC);

CREATE INDEX IX_exports_user_exported_at
    ON dbo.exports(exported_by_user_id, exported_at DESC);

ALTER TABLE dbo.audit_logs
ADD CONSTRAINT FK_audit_logs_exports
    FOREIGN KEY (export_id) REFERENCES dbo.exports(id) ON DELETE SET NULL;

CREATE INDEX IX_audit_logs_export_id
    ON dbo.audit_logs(export_id);

/* ------------------------------------------------------------------------- */
/* QA                                                                        */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.qa_reviews
(
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT PK_qa_reviews PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    box VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    profile VARCHAR(255) NOT NULL,
    scanned_by VARCHAR(255) NOT NULL,
    documents INT NOT NULL,
    pages INT NOT NULL,
    assigned_at DATETIME2 NOT NULL,
    reviewed INT NOT NULL,
    issues INT NOT NULL,
    created_by_user_id INT NULL,
    assigned_to_user_id INT NULL,
    started_at DATETIME2 NULL,
    completed_at DATETIME2 NULL,
    expires_at DATETIME2 NULL,
    last_updated_at DATETIME2 NOT NULL,
    CONSTRAINT FK_qa_reviews_session FOREIGN KEY (session_id) REFERENCES dbo.scan_sessions(id),
    CONSTRAINT FK_qa_reviews_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_qa_reviews_assigned_to_user FOREIGN KEY (assigned_to_user_id) REFERENCES dbo.users(id),
    CONSTRAINT UQ_qa_reviews_session_id UNIQUE (session_id)
);

CREATE INDEX IX_qa_reviews_assigned_to_status
    ON dbo.qa_reviews(assigned_to_user_id, status, assigned_at DESC);

CREATE INDEX IX_qa_reviews_created_by_status
    ON dbo.qa_reviews(created_by_user_id, status, assigned_at DESC);

CREATE TABLE dbo.qa_review_pages
(
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT PK_qa_review_pages PRIMARY KEY,
    qa_review_id UNIQUEIDENTIFIER NOT NULL,
    box VARCHAR(255) NOT NULL,
    profile VARCHAR(255) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_number INT NOT NULL,
    page_number INT NOT NULL,
    global_page_number INT NOT NULL,
    page_status VARCHAR(32) NOT NULL,
    rotation_degrees INT NOT NULL,
    comment NVARCHAR(MAX) NOT NULL,
    source_reference VARCHAR(1024) NOT NULL,
    display_content NVARCHAR(MAX) NOT NULL,
    page_readable BIT NOT NULL,
    rotation_correct BIT NOT NULL,
    split_correct BIT NOT NULL,
    page_count_correct BIT NOT NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT FK_qa_review_pages_review
        FOREIGN KEY (qa_review_id) REFERENCES dbo.qa_reviews(id) ON DELETE CASCADE
);

CREATE INDEX IX_qa_review_pages_review_document_page
    ON dbo.qa_review_pages(qa_review_id, document_number, page_number, global_page_number);

/* ------------------------------------------------------------------------- */
/* User notifications                                                        */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.user_notifications
(
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT PK_user_notifications PRIMARY KEY,
    user_id INT NOT NULL,
    qa_review_id UNIQUEIDENTIFIER NULL,
    title VARCHAR(255) NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL,
    read_at DATETIME2 NULL,
    CONSTRAINT FK_user_notifications_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_user_notifications_review
        FOREIGN KEY (qa_review_id) REFERENCES dbo.qa_reviews(id) ON DELETE SET NULL
);

CREATE INDEX IX_user_notifications_user_created_at
    ON dbo.user_notifications(user_id, created_at DESC);

/* ------------------------------------------------------------------------- */
/* Saved scan progress                                                       */
/* ------------------------------------------------------------------------- */

CREATE TABLE dbo.scan_saved_progress
(
    session_id VARCHAR(36) NOT NULL
        CONSTRAINT PK_scan_saved_progress PRIMARY KEY,
    box VARCHAR(255) NOT NULL,
    profile VARCHAR(255) NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_by_user_id INT NULL,
    saved_at DATETIME2 NOT NULL,
    CONSTRAINT FK_scan_saved_progress_session
        FOREIGN KEY (session_id) REFERENCES dbo.scan_sessions(id) ON DELETE CASCADE,
    CONSTRAINT FK_scan_saved_progress_user
        FOREIGN KEY (created_by_user_id) REFERENCES dbo.users(id)
);

CREATE INDEX IX_scan_saved_progress_user_saved_at
    ON dbo.scan_saved_progress(created_by_user_id, saved_at DESC);

CREATE TABLE dbo.scan_saved_progress_pages
(
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT PK_scan_saved_progress_pages PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    page_order INT NOT NULL,
    reference_id INT NOT NULL,
    file_id INT NOT NULL,
    document_number INT NOT NULL,
    is_barcode BIT NOT NULL,
    rotation_degrees INT NOT NULL,
    needs_rescan BIT NOT NULL,
    split_reason_after VARCHAR(255) NULL,
    source_reference VARCHAR(1024) NOT NULL,
    display_content NVARCHAR(MAX) NOT NULL,
    preview_content NVARCHAR(MAX) NOT NULL,
    CONSTRAINT FK_scan_saved_progress_pages_session
        FOREIGN KEY (session_id) REFERENCES dbo.scan_saved_progress(session_id) ON DELETE CASCADE
);

CREATE INDEX IX_scan_saved_progress_pages_session_order
    ON dbo.scan_saved_progress_pages(session_id, page_order);
