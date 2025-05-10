-- Create questionnaire_responses table
CREATE TABLE gatekeeper.questionnaire_result
(
    id                 VARCHAR(36) PRIMARY KEY,
    form_definition_id VARCHAR(36) NOT NULL,
    user_id            VARCHAR(255), -- Assuming user_id can be a string of variable length, adjust if needed
    response_data JSONB NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255)
    -- If you have a foreign key constraint to form_definitions:
    -- CONSTRAINT fk_form_definition FOREIGN KEY (form_definition_id) REFERENCES gatekeeper.form_definitions(id)
);

-- Optional: Indexes for faster lookups
CREATE INDEX idx_questionnaire_responses_form_definition_id ON gatekeeper.questionnaire_result (form_definition_id);
CREATE INDEX idx_questionnaire_responses_user_id ON gatekeeper.questionnaire_result (user_id); 