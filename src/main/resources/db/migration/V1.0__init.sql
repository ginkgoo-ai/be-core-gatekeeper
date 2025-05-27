-- V1__Initial_Schema.sql
CREATE TABLE gatekeeper.form_definitions
(
    id              VARCHAR(36) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    form_type VARCHAR(255) NOT NULL DEFAULT 'GENERIC_FORM',
    description     VARCHAR(1000),
    status          VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    version         VARCHAR(50)  NOT NULL DEFAULT '1.0.0',
    target_audience VARCHAR(255),
    initial_logic JSONB,
    submission_logic JSONB,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT uk_form_definitions_name UNIQUE (name)
);

-- Create index on status for filtering
CREATE INDEX idx_form_definitions_status ON gatekeeper.form_definitions (status);


-- Create section_definitions table
CREATE TABLE gatekeeper.section_definitions
(
    id                 VARCHAR(36) PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    display_order      INT,
    condition          VARCHAR(1000),
    form_definition_id VARCHAR(36)  NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255)
);
CREATE INDEX idx_section_form_definition_id ON gatekeeper.section_definitions (form_definition_id);
CREATE INDEX idx_section_display_order ON gatekeeper.section_definitions (form_definition_id, display_order);

-- Create field_definitions table
CREATE TABLE gatekeeper.field_definitions
(
    id                    VARCHAR(36) PRIMARY KEY,
    name                  VARCHAR(255) NOT NULL, -- Backend key
    label                 VARCHAR(255) NOT NULL,
    field_type            VARCHAR(50)  NOT NULL,
    placeholder           VARCHAR(255),
    default_value         TEXT,                  -- Can be long for some types
    options_source_type   VARCHAR(50),
    static_options JSONB,
    api_endpoint          VARCHAR(2048),
    ui_properties JSONB,
    condition             VARCHAR(1000),
    dependencies JSONB,
    display_order         INT,
    section_definition_id VARCHAR(36)  NOT NULL,
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);
CREATE INDEX idx_field_section_definition_id ON gatekeeper.field_definitions (section_definition_id);
CREATE INDEX idx_field_display_order ON gatekeeper.field_definitions (section_definition_id, display_order);

-- Create validation_rules table
CREATE TABLE gatekeeper.validation_rules
(
    id                  VARCHAR(36) PRIMARY KEY,
    type                VARCHAR(50)  NOT NULL,
    value               TEXT, -- Can be long for regex etc.
    error_message       VARCHAR(500) NOT NULL,
    custom_function     VARCHAR(255),
    field_definition_id VARCHAR(36)  NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);
CREATE INDEX idx_validation_field_definition_id ON gatekeeper.validation_rules (field_definition_id); 