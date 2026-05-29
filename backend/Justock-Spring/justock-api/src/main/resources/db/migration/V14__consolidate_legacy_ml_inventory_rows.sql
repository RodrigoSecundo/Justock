WITH legacy_products AS (
    SELECT
        legacy.id_produto AS legacy_id,
        legacy.usuario,
        legacy.marketplace_resource_id,
        legacy.categoria,
        legacy.marca,
        legacy.nome_do_produto,
        legacy.codigo_de_barras,
        legacy.preco
    FROM estoque legacy
    WHERE UPPER(COALESCE(legacy.marcador, '')) = 'ML'
      AND UPPER(COALESCE(legacy.marketplace_source, '')) = 'MERCADO_LIVRE'
      AND COALESCE(legacy.marketplace_resource_id, '') <> ''
),
shadow_pairs AS (
    SELECT
        legacy.legacy_id,
        shadow.id_produto AS shadow_id
    FROM legacy_products legacy
    JOIN estoque shadow
      ON shadow.usuario = legacy.usuario
     AND shadow.marketplace_resource_id = legacy.marketplace_resource_id
     AND UPPER(COALESCE(shadow.marcador, '')) = 'ANUNCIO_ESTOQUE'
    LEFT JOIN marketplace_listing listing
      ON listing.usuario = legacy.usuario
     AND listing.marketplace_source = 'MERCADO_LIVRE'
     AND listing.marketplace_resource_id = legacy.marketplace_resource_id
    WHERE legacy.legacy_id <> shadow.id_produto
      AND COALESCE(listing.produto_vinculado_id, 0) = 0
)
UPDATE estoque shadow
SET categoria = CASE
        WHEN shadow.categoria IS NULL OR BTRIM(shadow.categoria) = '' OR UPPER(BTRIM(shadow.categoria)) = 'N/A'
            THEN legacy.categoria
        ELSE shadow.categoria
    END,
    marca = CASE
        WHEN shadow.marca IS NULL OR BTRIM(shadow.marca) = '' OR UPPER(BTRIM(shadow.marca)) = 'N/A'
            THEN legacy.marca
        ELSE shadow.marca
    END,
    codigo_de_barras = CASE
        WHEN shadow.codigo_de_barras IS NULL OR BTRIM(shadow.codigo_de_barras) = '' OR UPPER(BTRIM(shadow.codigo_de_barras)) = 'N/A'
            THEN legacy.codigo_de_barras
        ELSE shadow.codigo_de_barras
    END,
    preco = COALESCE(shadow.preco, legacy.preco)
FROM shadow_pairs pairs
JOIN estoque legacy ON legacy.id_produto = pairs.legacy_id
WHERE shadow.id_produto = pairs.shadow_id;

WITH legacy_products AS (
    SELECT
        legacy.id_produto AS legacy_id,
        legacy.usuario,
        legacy.marketplace_resource_id
    FROM estoque legacy
    WHERE UPPER(COALESCE(legacy.marcador, '')) = 'ML'
      AND UPPER(COALESCE(legacy.marketplace_source, '')) = 'MERCADO_LIVRE'
      AND COALESCE(legacy.marketplace_resource_id, '') <> ''
),
shadow_pairs AS (
    SELECT
        legacy.legacy_id,
        shadow.id_produto AS shadow_id
    FROM legacy_products legacy
    JOIN estoque shadow
      ON shadow.usuario = legacy.usuario
     AND shadow.marketplace_resource_id = legacy.marketplace_resource_id
     AND UPPER(COALESCE(shadow.marcador, '')) = 'ANUNCIO_ESTOQUE'
    LEFT JOIN marketplace_listing listing
      ON listing.usuario = legacy.usuario
     AND listing.marketplace_source = 'MERCADO_LIVRE'
     AND listing.marketplace_resource_id = legacy.marketplace_resource_id
    WHERE legacy.legacy_id <> shadow.id_produto
      AND COALESCE(listing.produto_vinculado_id, 0) = 0
)
UPDATE estoque_pedido target
SET quantidade = COALESCE(target.quantidade, 0) + COALESCE(source.quantidade, 0),
    subtotal = COALESCE(target.subtotal, 0) + COALESCE(source.subtotal, 0),
    preco_unitario = COALESCE(target.preco_unitario, source.preco_unitario),
    id_item_marketplace = COALESCE(target.id_item_marketplace, source.id_item_marketplace),
    item_status = COALESCE(target.item_status, source.item_status)
FROM shadow_pairs pairs
JOIN estoque_pedido source
  ON source.id_produto = pairs.legacy_id
WHERE target.id_produto = pairs.shadow_id
  AND target.id_pedido = source.id_pedido;

WITH legacy_products AS (
    SELECT
        legacy.id_produto AS legacy_id,
        legacy.usuario,
        legacy.marketplace_resource_id
    FROM estoque legacy
    WHERE UPPER(COALESCE(legacy.marcador, '')) = 'ML'
      AND UPPER(COALESCE(legacy.marketplace_source, '')) = 'MERCADO_LIVRE'
      AND COALESCE(legacy.marketplace_resource_id, '') <> ''
),
shadow_pairs AS (
    SELECT
        legacy.legacy_id,
        shadow.id_produto AS shadow_id
    FROM legacy_products legacy
    JOIN estoque shadow
      ON shadow.usuario = legacy.usuario
     AND shadow.marketplace_resource_id = legacy.marketplace_resource_id
     AND UPPER(COALESCE(shadow.marcador, '')) = 'ANUNCIO_ESTOQUE'
    LEFT JOIN marketplace_listing listing
      ON listing.usuario = legacy.usuario
     AND listing.marketplace_source = 'MERCADO_LIVRE'
     AND listing.marketplace_resource_id = legacy.marketplace_resource_id
    WHERE legacy.legacy_id <> shadow.id_produto
      AND COALESCE(listing.produto_vinculado_id, 0) = 0
)
UPDATE estoque_pedido source
SET id_produto = pairs.shadow_id
FROM shadow_pairs pairs
WHERE source.id_produto = pairs.legacy_id
  AND NOT EXISTS (
      SELECT 1
      FROM estoque_pedido target
      WHERE target.id_produto = pairs.shadow_id
        AND target.id_pedido = source.id_pedido
  );

WITH legacy_products AS (
    SELECT
        legacy.id_produto AS legacy_id,
        legacy.usuario,
        legacy.marketplace_resource_id
    FROM estoque legacy
    WHERE UPPER(COALESCE(legacy.marcador, '')) = 'ML'
      AND UPPER(COALESCE(legacy.marketplace_source, '')) = 'MERCADO_LIVRE'
      AND COALESCE(legacy.marketplace_resource_id, '') <> ''
),
shadow_pairs AS (
    SELECT
        legacy.legacy_id,
        shadow.id_produto AS shadow_id
    FROM legacy_products legacy
    JOIN estoque shadow
      ON shadow.usuario = legacy.usuario
     AND shadow.marketplace_resource_id = legacy.marketplace_resource_id
     AND UPPER(COALESCE(shadow.marcador, '')) = 'ANUNCIO_ESTOQUE'
    LEFT JOIN marketplace_listing listing
      ON listing.usuario = legacy.usuario
     AND listing.marketplace_source = 'MERCADO_LIVRE'
     AND listing.marketplace_resource_id = legacy.marketplace_resource_id
    WHERE legacy.legacy_id <> shadow.id_produto
      AND COALESCE(listing.produto_vinculado_id, 0) = 0
)
DELETE FROM estoque_pedido source
USING shadow_pairs pairs
WHERE source.id_produto = pairs.legacy_id;

WITH legacy_products AS (
    SELECT
        legacy.id_produto AS legacy_id,
        legacy.usuario,
        legacy.marketplace_resource_id
    FROM estoque legacy
    WHERE UPPER(COALESCE(legacy.marcador, '')) = 'ML'
      AND UPPER(COALESCE(legacy.marketplace_source, '')) = 'MERCADO_LIVRE'
      AND COALESCE(legacy.marketplace_resource_id, '') <> ''
),
shadow_pairs AS (
    SELECT
        legacy.legacy_id,
        shadow.id_produto AS shadow_id
    FROM legacy_products legacy
    JOIN estoque shadow
      ON shadow.usuario = legacy.usuario
     AND shadow.marketplace_resource_id = legacy.marketplace_resource_id
     AND UPPER(COALESCE(shadow.marcador, '')) = 'ANUNCIO_ESTOQUE'
    LEFT JOIN marketplace_listing listing
      ON listing.usuario = legacy.usuario
     AND listing.marketplace_source = 'MERCADO_LIVRE'
     AND listing.marketplace_resource_id = legacy.marketplace_resource_id
    WHERE legacy.legacy_id <> shadow.id_produto
      AND COALESCE(listing.produto_vinculado_id, 0) = 0
)
DELETE FROM estoque legacy
USING shadow_pairs pairs
WHERE legacy.id_produto = pairs.legacy_id
  AND NOT EXISTS (
      SELECT 1
      FROM estoque_pedido order_items
      WHERE order_items.id_produto = legacy.id_produto
  );

UPDATE estoque legacy
SET marcador = 'ANUNCIO_ESTOQUE'
WHERE UPPER(COALESCE(legacy.marcador, '')) = 'ML'
  AND UPPER(COALESCE(legacy.marketplace_source, '')) = 'MERCADO_LIVRE'
  AND COALESCE(legacy.marketplace_resource_id, '') <> ''
  AND EXISTS (
      SELECT 1
      FROM marketplace_listing listing
      WHERE listing.usuario = legacy.usuario
        AND listing.marketplace_source = 'MERCADO_LIVRE'
        AND listing.marketplace_resource_id = legacy.marketplace_resource_id
        AND listing.produto_vinculado_id IS NULL
  )
  AND NOT EXISTS (
      SELECT 1
      FROM estoque shadow
      WHERE shadow.usuario = legacy.usuario
        AND shadow.marketplace_resource_id = legacy.marketplace_resource_id
        AND shadow.id_produto <> legacy.id_produto
        AND UPPER(COALESCE(shadow.marcador, '')) = 'ANUNCIO_ESTOQUE'
  );