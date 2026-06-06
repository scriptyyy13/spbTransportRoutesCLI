CREATE INDEX idx_route_stops_route_and_order ON route_stops (route_id, stop_number);
CREATE INDEX idx_routes_name_exact ON routes (route_long_name);
CREATE INDEX idx_stops_name_exact ON stops (name);
CREATE INDEX idx_routes_route_id ON routes (route_id);
CREATE INDEX idx_stops_stop_id ON stops (stop_id);