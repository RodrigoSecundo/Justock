CREATE TABLE IF NOT EXISTS estoque_pedido (
    id_produto INTEGER NOT NULL,
    id_pedido INTEGER NOT NULL,
    quantidade INTEGER,
    preco_unitario NUMERIC(19, 2),
    subtotal NUMERIC(19, 2),
    id_item_marketplace VARCHAR(255),
    item_status VARCHAR(255),
    CONSTRAINT pk_estoque_pedido PRIMARY KEY (id_produto, id_pedido)
);

CREATE INDEX IF NOT EXISTS idx_estoque_pedido_id_pedido
    ON estoque_pedido (id_pedido);