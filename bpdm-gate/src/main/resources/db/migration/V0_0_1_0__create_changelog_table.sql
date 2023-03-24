CREATE SEQUENCE IF NOT EXISTS bpdm_gate_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE changelog_entries
(
    id            BIGINT                      NOT NULL,
    external_id    VARCHAR(255)                NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    business_partner_type VARCHAR(255)         NOT NULL,
    CONSTRAINT pk_changelog PRIMARY KEY (id)
);

