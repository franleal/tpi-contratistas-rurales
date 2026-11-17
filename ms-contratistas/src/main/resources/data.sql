-- Datos iniciales de ms-contratistas (perfil de demostración: se cargan en cada arranque).
-- Las fechas son relativas al día de arranque para que los casos no dependan del calendario.
-- Con la base recién creada: máquinas 1 (contratista1) y 2 (contratista2), aplicadores 1, 2 y 3.

INSERT INTO contratista (sub, razon_social, cuit) VALUES
  ('22222222-0000-0000-0000-000000000001', 'Aplicaciones del Centro SRL', '30712345679'),
  ('22222222-0000-0000-0000-000000000002', 'Pulverizaciones del Sur SA', '30798765432');

-- Base de la máquina 1 a ~10 km del lote 1 (traslado corto); máquina 2 en Córdoba capital.
INSERT INTO maquina (contratista_id, tipo, descripcion, ancho_m, rendimiento_ha_h, base_lat, base_lon) VALUES
  ((SELECT id FROM contratista WHERE cuit = '30712345679'), 'PULVERIZADORA', 'Pla MAP II 3250', 28.00, 25.00,
   -31.680000, -63.880000),
  ((SELECT id FROM contratista WHERE cuit = '30798765432'), 'PULVERIZADORA', 'Metalfor Futura 3200', 27.00, 20.00,
   -31.420000, -64.180000);

INSERT INTO aplicador (contratista_id, nombre, documento) VALUES
  ((SELECT id FROM contratista WHERE cuit = '30712345679'), 'Juan Pérez', '20111222'),
  ((SELECT id FROM contratista WHERE cuit = '30798765432'), 'Marta Gómez', '27333444'),
  ((SELECT id FROM contratista WHERE cuit = '30798765432'), 'Luis Sosa', '23555666');

-- Habilitaciones:
--  * máquina 1 y aplicador 1: VALIDADAS por un año (caso feliz)
--  * máquina 2: VALIDADA; aplicador 2: carnet VALIDADO que vence en 2 días (RN-02/RN-04 para trabajos posteriores)
--  * aplicador 3: carnet PENDIENTE (para que el ADMIN lo apruebe o rechace, F-08)
INSERT INTO habilitacion (tipo, maquina_id, aplicador_id, numero, vencimiento, estado, cargada_en) VALUES
  ('INSPECCION_MAQUINA', (SELECT id FROM maquina WHERE descripcion = 'Pla MAP II 3250'), NULL, 'ITV-CBA-0001',
   CAST(CURRENT_DATE + INTERVAL '365' DAY AS DATE), 'VALIDADA', CURRENT_TIMESTAMP),
  ('CARNET_APLICADOR', NULL, (SELECT id FROM aplicador WHERE documento = '20111222'), 'CAR-CBA-1001',
   CAST(CURRENT_DATE + INTERVAL '365' DAY AS DATE), 'VALIDADA', CURRENT_TIMESTAMP),
  ('INSPECCION_MAQUINA', (SELECT id FROM maquina WHERE descripcion = 'Metalfor Futura 3200'), NULL, 'ITV-CBA-0002',
   CAST(CURRENT_DATE + INTERVAL '365' DAY AS DATE), 'VALIDADA', CURRENT_TIMESTAMP),
  ('CARNET_APLICADOR', NULL, (SELECT id FROM aplicador WHERE documento = '27333444'), 'CAR-CBA-2002',
   CAST(CURRENT_DATE + INTERVAL '2' DAY AS DATE), 'VALIDADA', CURRENT_TIMESTAMP),
  ('CARNET_APLICADOR', NULL, (SELECT id FROM aplicador WHERE documento = '23555666'), 'CAR-CBA-2003',
   CAST(CURRENT_DATE + INTERVAL '365' DAY AS DATE), 'PENDIENTE', CURRENT_TIMESTAMP);

-- Disponibilidad: ambas máquinas desde mañana 05:00 hasta dentro de 7 días a las 21:00 (dentro del horizonte)
INSERT INTO bloque_disponibilidad (maquina_id, desde, hasta) VALUES
  ((SELECT id FROM maquina WHERE descripcion = 'Pla MAP II 3250'),
   CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '1' DAY + INTERVAL '5' HOUR,
   CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '7' DAY + INTERVAL '21' HOUR),
  ((SELECT id FROM maquina WHERE descripcion = 'Metalfor Futura 3200'),
   CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '1' DAY + INTERVAL '5' HOUR,
   CAST(CURRENT_DATE AS TIMESTAMP) + INTERVAL '7' DAY + INTERVAL '21' HOUR);
