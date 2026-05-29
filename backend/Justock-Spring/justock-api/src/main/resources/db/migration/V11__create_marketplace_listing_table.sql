CREATE TABLE IF NOT EXISTS marketplace_listing (
    id BIGSERIAL PRIMARY KEY,
    usuario INTEGER NOT NULL,
    marketplace_source VARCHAR(64) NOT NULL,
    marketplace_resource_id VARCHAR(255) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    categoria VARCHAR(255),
    marca VARCHAR(255),
    codigo_de_barras VARCHAR(255),
    preco NUMERIC(19, 2),
    quantidade_disponivel INTEGER,
    produto_vinculado_id INTEGER,
    separado_manutencao BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_marketplace_listing_usuario_source_resource
        UNIQUE (usuario, marketplace_source, marketplace_resource_id)
);

CREATE INDEX IF NOT EXISTS idx_marketplace_listing_usuario
    ON marketplace_listing (usuario);

CREATE INDEX IF NOT EXISTS idx_marketplace_listing_produto_vinculado
    ON marketplace_listing (produto_vinculado_id);