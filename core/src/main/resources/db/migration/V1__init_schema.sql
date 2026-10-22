CREATE TABLE sync_run (
    id UUID NOT NULL,
    source VARCHAR(50) NOT NULL,
    trigger VARCHAR(20) NOT NULL CHECK (trigger IN ('SCHEDULED', 'MANUAL')),
    archive_url VARCHAR(500) NOT NULL,
    archive_sha256 VARCHAR(64),
    -- The Assemblée's servers don't all serve the latest archive: one older than the last applied run is skipped.
    archive_last_modified TIMESTAMP,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SKIPPED', 'SUCCEEDED', 'FAILED')),
    created INTEGER NOT NULL DEFAULT 0,
    updated INTEGER NOT NULL DEFAULT 0,
    unchanged INTEGER NOT NULL DEFAULT 0,
    failed INTEGER NOT NULL DEFAULT 0,
    error TEXT,
    PRIMARY KEY (id)
);

-- One row per source file: its hash decides whether the next run reprocesses it, and its S3 key points to the raw JSON.
CREATE TABLE raw_record (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    source VARCHAR(50) NOT NULL,
    entity VARCHAR(50) NOT NULL, -- source vocabulary, e.g. 'dossierParlementaire', 'document'
    external_id VARCHAR(50) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    s3_version_id VARCHAR(255),
    last_seen_run_id UUID NOT NULL REFERENCES sync_run (id),
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (source, entity, external_id)
);

-- A legislative dossier. Every dossier is stored; is_law tells laws apart from resolutions, reports, inquiries...
CREATE TABLE law (
    id UUID NOT NULL,
    jurisdiction_code VARCHAR(2) NOT NULL,
    source VARCHAR(50) NOT NULL,
    external_id VARCHAR(50) NOT NULL,
    legislature INTEGER NOT NULL,
    title TEXT NOT NULL,
    short_title TEXT, -- titrePrincipalCourt of the initial text, filled once its document is synced
    procedure_code VARCHAR(10) NOT NULL,
    procedure_label VARCHAR(255) NOT NULL,
    is_law BOOLEAN NOT NULL,
    initiator_type VARCHAR(20) NOT NULL CHECK (initiator_type IN ('GOVERNMENT', 'DEPUTY', 'SENATOR', 'OTHER')),
    initiator_refs VARCHAR(50)[] NOT NULL,
    initial_text_ref VARCHAR(50),
    -- Null for dossiers that aren't laws. STALLED isn't stored: it depends on today's date, so the API derives it from last_act_date.
    status VARCHAR(30) CHECK (status IN ('DEPOSITED', 'IN_COMMITTEE', 'IN_PLENARY', 'IN_SENATE', 'CMP', 'NEW_READING',
                                         'ADOPTED', 'CONSTITUTIONAL_COUNCIL', 'PROMULGATED', 'REJECTED', 'WITHDRAWN', 'LAPSED')),
    current_phase_code VARCHAR(20),
    current_stage_code VARCHAR(50),
    current_stage_label VARCHAR(255),
    first_act_date DATE,
    last_act_date DATE, -- latest step that has already happened, never a scheduled one
    next_scheduled_date DATE,
    promulgation_date DATE,
    law_number VARCHAR(20),
    law_title TEXT,
    jo_date DATE,
    jo_number VARCHAR(20),
    nor VARCHAR(30),
    legifrance_url VARCHAR(500),
    an_url VARCHAR(500) NOT NULL,
    senat_url VARCHAR(500),
    lead_committee_ref VARCHAR(50),
    merged_into_external_id VARCHAR(50),
    merge_cause VARCHAR(100),
    first_seen_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (source, external_id)
);

-- Unchanged dossiers whose scheduled step has passed are re-derived each night.
CREATE INDEX law_next_scheduled_date_idx ON law (next_scheduled_date);

-- The dossier's acts, flattened in tree order. Rewritten whole when the dossier changes.
CREATE TABLE law_step (
    id UUID NOT NULL,
    law_id UUID NOT NULL REFERENCES law (id) ON DELETE CASCADE,
    parent_step_id UUID REFERENCES law_step (id) ON DELETE CASCADE,
    external_id VARCHAR(50) NOT NULL, -- not unique: a few acts are shared by two dossiers
    position INTEGER NOT NULL,
    depth INTEGER NOT NULL,
    phase_code VARCHAR(20) NOT NULL, -- top-level act, e.g. AN1, SN1, CMP, CC, PROM
    code VARCHAR(50) NOT NULL,
    label VARCHAR(255),
    act_date DATE,
    is_scheduled BOOLEAN NOT NULL,
    organe_ref VARCHAR(50),
    associated_text_ref VARCHAR(50),
    adopted_text_ref VARCHAR(50),
    conclusion_code VARCHAR(20),
    conclusion_label VARCHAR(255),
    decision_number VARCHAR(20),
    decision_url VARCHAR(500),
    PRIMARY KEY (id)
);

CREATE INDEX law_step_law_id_idx ON law_step (law_id, position);

-- Ballots for the vote service: the scrutins attached to the dossier's acts.
CREATE TABLE law_vote_link (
    id UUID NOT NULL,
    law_id UUID NOT NULL REFERENCES law (id) ON DELETE CASCADE,
    step_id UUID NOT NULL REFERENCES law_step (id) ON DELETE CASCADE,
    source VARCHAR(50) NOT NULL, -- the scrutin's source: a few dossiers reference Senate scrutins (VTSNR...)
    scrutin_external_id VARCHAR(50) NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('TEXT', 'MOTION')),
    is_decisive BOOLEAN NOT NULL, -- the Assemblée's last vote on the whole text
    PRIMARY KEY (id),
    UNIQUE (law_id, scrutin_external_id)
);

-- The vote service looks laws up by scrutin.
CREATE INDEX law_vote_link_scrutin_idx ON law_vote_link (source, scrutin_external_id);

CREATE TABLE law_document (
    id UUID NOT NULL,
    source VARCHAR(50) NOT NULL,
    external_id VARCHAR(50) NOT NULL,
    law_id UUID REFERENCES law (id) ON DELETE SET NULL, -- null until the dossier itself is synced
    dossier_external_id VARCHAR(50) NOT NULL,
    chamber VARCHAR(2) NOT NULL CHECK (chamber IN ('AN', 'SN')),
    legislature INTEGER, -- absent on Senate documents
    type_code VARCHAR(10) NOT NULL,
    type_label VARCHAR(255) NOT NULL,
    subtype_code VARCHAR(20),
    subtype_label VARCHAR(255),
    title TEXT NOT NULL,
    short_title TEXT,
    creation_date DATE,
    deposit_date DATE,
    publication_date DATE,
    notice_number VARCHAR(20),
    page_count INTEGER,
    author_refs VARCHAR(50)[] NOT NULL,
    first_seen_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (source, external_id)
);

CREATE INDEX law_document_law_id_idx ON law_document (law_id);

CREATE TABLE category (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

-- First categorisation rule: the Assemblée committee that examines the text (organeRef of AN1-COM-FOND).
CREATE TABLE committee_category (
    organe_ref VARCHAR(50) NOT NULL,
    category_id UUID NOT NULL REFERENCES category (id),
    PRIMARY KEY (organe_ref)
);

CREATE TABLE law_category (
    law_id UUID NOT NULL REFERENCES law (id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES category (id),
    origin VARCHAR(20) NOT NULL CHECK (origin IN ('COMMITTEE_RULE', 'AI', 'MANUAL')),
    PRIMARY KEY (law_id, category_id)
);
