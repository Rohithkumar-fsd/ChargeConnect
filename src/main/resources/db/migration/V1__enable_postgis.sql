-- Enable PostGIS so later charger searches can use geography, ST_DWithin, and ST_Distance.
-- IF NOT EXISTS is safe when the postgis/postgis Docker image already created the extension.
CREATE EXTENSION IF NOT EXISTS postgis;
