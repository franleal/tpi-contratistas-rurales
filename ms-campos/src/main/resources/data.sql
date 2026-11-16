-- Datos iniciales de ms-campos (perfil de demostración: se cargan en cada arranque).
-- Parámetros de zona por provincia (RN-08). La clave es el nombre que devuelve Georef.
-- Valores de referencia, modificables por el ADMIN con PUT /api/admin/zonas/{provincia}.
-- La distancia se mide al centroide de la localidad (no al borde del ejido), por eso es
-- mayor que la franja legal medida desde el límite urbano.
INSERT INTO parametro_zona (provincia, distancia_exclusion_m, distancia_amortiguamiento_m, categorias_permitidas) VALUES
  ('Córdoba',              1500, 3000, 'III,IV'),
  ('Buenos Aires',         1000, 2000, 'III,IV'),
  ('Santa Fe',             1000, 3000, 'III,IV'),
  ('Entre Ríos',           1000, 3000, 'III,IV'),
  ('La Pampa',             1000, 2500, 'III,IV'),
  ('San Luis',             1000, 2000, 'III,IV'),
  ('Santiago del Estero',  1000, 2000, 'III,IV'),
  ('Chaco',                1000, 2000, 'III,IV'),
  ('Tucumán',              1000, 2000, 'III,IV'),
  ('Salta',                1000, 2000, 'III,IV');

-- DEMO: productores, establecimientos y lotes ya clasificados (sin llamar a Georef).
-- Con la base recién creada los ids son 1, 2, 3 y 4 (los usa la colección Postman).
INSERT INTO productor (sub, nombre, email) VALUES
  ('11111111-0000-0000-0000-000000000001', 'Pedro Productor', 'productor1@agro.test'),
  ('11111111-0000-0000-0000-000000000002', 'Paula Productora', 'productor2@agro.test');

INSERT INTO establecimiento (productor_id, nombre, provincia) VALUES
  ((SELECT id FROM productor WHERE sub = '11111111-0000-0000-0000-000000000001'), 'La Esperanza', 'Córdoba'),
  ((SELECT id FROM productor WHERE sub = '11111111-0000-0000-0000-000000000002'), 'El Ombú', 'Córdoba');

-- Lote 1 LIBRE (caso feliz), lote 2 AMORTIGUAMIENTO (RN-07), lote 3 EXCLUSION (RN-06), lote 4 de productor2 (propiedad)
INSERT INTO lote (establecimiento_id, nombre, lat, lon, superficie_ha, cultivo, provincia, departamento, zona,
                  localidad_cercana, distancia_localidad_m) VALUES
  ((SELECT id FROM establecimiento WHERE nombre = 'La Esperanza'), 'Lote Norte', -31.620000, -63.950000, 100.00,
   'Soja', 'Córdoba', 'Río Segundo', 'LIBRE', 'Pilar', 8200),
  ((SELECT id FROM establecimiento WHERE nombre = 'La Esperanza'), 'Lote del Arroyo', -31.660000, -63.900000, 60.00,
   'Maíz', 'Córdoba', 'Río Segundo', 'AMORTIGUAMIENTO', 'Pilar', 2400),
  ((SELECT id FROM establecimiento WHERE nombre = 'La Esperanza'), 'Lote del Pueblo', -31.672000, -63.885000, 25.00,
   'Trigo', 'Córdoba', 'Río Segundo', 'EXCLUSION', 'Pilar', 900),
  ((SELECT id FROM establecimiento WHERE nombre = 'El Ombú'), 'Lote Sur', -31.800000, -63.800000, 80.00,
   'Soja', 'Córdoba', 'Río Segundo', 'LIBRE', 'Laguna Larga', 9100);
