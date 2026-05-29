WITH normalized_listing_brand AS (
    SELECT ml.id,
           CASE
               WHEN ml.titulo ILIKE '%asus%' THEN 'ASUS'
               WHEN ml.titulo ILIKE '%galax%' THEN 'Galax'
               WHEN ml.titulo ILIKE '%gigabyte%' THEN 'Gigabyte'
               WHEN ml.titulo ILIKE '%msi%' THEN 'MSI'
               WHEN ml.titulo ILIKE '%kingston%' THEN 'Kingston'
               WHEN ml.titulo ILIKE '%corsair%' THEN 'Corsair'
               WHEN ml.titulo ILIKE '%samsung%' THEN 'Samsung'
               WHEN ml.titulo ILIKE '%seagate%' THEN 'Seagate'
               WHEN ml.titulo ILIKE '%netac%' THEN 'Netac'
               WHEN ml.titulo ILIKE '%intel%' THEN 'Intel'
               WHEN ml.titulo ILIKE '%amd%' THEN 'AMD'
               WHEN ml.titulo ILIKE '%nvidia%' THEN 'NVIDIA'
               WHEN ml.titulo ILIKE '%western digital%' OR ml.titulo ILIKE '% wd %' OR ml.titulo ILIKE 'wd %' OR ml.titulo ILIKE '% wd' THEN 'WD'
               WHEN ml.titulo ILIKE '%g skill%' OR ml.titulo ILIKE '%gskill%' THEN 'G.Skill'
               WHEN ml.titulo ILIKE '%xpg%' THEN 'XPG'
               ELSE NULL
           END AS marca_normalizada
      FROM marketplace_listing ml
     WHERE upper(coalesce(ml.marketplace_source, '')) = 'MERCADO_LIVRE'
       AND (ml.marca IS NULL OR btrim(ml.marca) = '' OR upper(btrim(ml.marca)) = 'N/A')
)
UPDATE marketplace_listing ml
   SET marca = normalized_listing_brand.marca_normalizada
  FROM normalized_listing_brand
 WHERE ml.id = normalized_listing_brand.id
   AND normalized_listing_brand.marca_normalizada IS NOT NULL;

UPDATE estoque p
   SET marca = ml.marca
  FROM marketplace_listing ml
 WHERE p.usuario = ml.usuario
   AND p.marketplace_resource_id = ml.marketplace_resource_id
   AND upper(coalesce(p.marketplace_source, '')) = 'MERCADO_LIVRE'
   AND upper(coalesce(p.marcador, '')) = 'ANUNCIO_ESTOQUE'
   AND upper(coalesce(ml.marketplace_source, '')) = 'MERCADO_LIVRE'
   AND ml.marca IS NOT NULL
   AND btrim(ml.marca) <> ''
   AND upper(btrim(ml.marca)) <> 'N/A'
   AND (p.marca IS NULL OR btrim(p.marca) = '' OR upper(btrim(p.marca)) = 'N/A');