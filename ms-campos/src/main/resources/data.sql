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
