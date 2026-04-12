ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS marketplace_resource_id VARCHAR(255);

ALTER TABLE pedido
    ADD COLUMN IF NOT EXISTS marketplace_source VARCHAR(255);

UPDATE pedido
SET marketplace_source = CASE id_pedido_marketplace
    WHEN 1 THEN 'MERCADO_LIVRE'
    WHEN 2 THEN 'AMAZON'
    WHEN 3 THEN 'SHOPEE'
    WHEN 4 THEN 'MANUAL'
    ELSE marketplace_source
END
WHERE marketplace_source IS NULL;
