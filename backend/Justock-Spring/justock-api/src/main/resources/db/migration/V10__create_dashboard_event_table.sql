CREATE TABLE dashboard_event (
    id BIGSERIAL PRIMARY KEY,
    usuario INTEGER NOT NULL,
    event_scope VARCHAR(20) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(255) NOT NULL,
    resource_type VARCHAR(40),
    resource_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    metadata_json TEXT NULL
);

CREATE INDEX idx_dashboard_event_usuario_scope_created
    ON dashboard_event (usuario, event_scope, created_at DESC);

CREATE INDEX idx_dashboard_event_alert_lookup
    ON dashboard_event (usuario, event_scope, event_type, resource_type, resource_id);