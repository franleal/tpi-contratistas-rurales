-- Datos iniciales de ms-trabajos (perfil de demostración, se recargan al arrancar)

-- F-09: umbrales climáticos de referencia (fila única, modificable por el ADMIN)
INSERT INTO umbral_climatico (id, viento_min_kmh, viento_max_kmh, rafaga_max_kmh, temperatura_max_c,
                              humedad_min_pct, horas_sin_lluvia_despues, horizonte_dias)
VALUES (1, 5.0, 15.0, 20.0, 30.0, 50.0, 3, 7);
