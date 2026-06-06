CREATE INDEX IF NOT EXISTS idx_route_stops_route_and_order ON route_stops (route_id, stop_number);
CREATE INDEX IF NOT EXISTS idx_routes_name_exact ON routes (route_long_name);
CREATE INDEX IF NOT EXISTS idx_stops_name_exact ON stops (name);