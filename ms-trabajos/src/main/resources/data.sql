-- Datos iniciales de ms-trabajos (perfil de demostración, se recargan al arrancar)

-- F-09: umbrales climáticos de referencia (fila única, modificable por el ADMIN)
INSERT INTO umbral_climatico (id, viento_min_kmh, viento_max_kmh, rafaga_max_kmh, temperatura_max_c,
                              humedad_min_pct, horas_sin_lluvia_despues, horizonte_dias)
VALUES (1, 5.0, 15.0, 20.0, 30.0, 50.0, 3, 7);

-- DEMO: agrónomos. agronomo1 con matrícula VALIDADA por un año; agronomo2 PENDIENTE (RN-01: 403 al firmar).
INSERT INTO agronomo (sub, nombre, matricula, provincia, vencimiento, estado) VALUES
  ('33333333-0000-0000-0000-000000000001', 'Ana Agronoma', 'CIAC-4521', 'Córdoba',
   CAST(CURRENT_DATE + INTERVAL '365' DAY AS DATE), 'VALIDADA'),
  ('33333333-0000-0000-0000-000000000002', 'Andres Agronomo', 'CIAC-7788', 'Córdoba',
   CAST(CURRENT_DATE + INTERVAL '365' DAY AS DATE), 'PENDIENTE');
