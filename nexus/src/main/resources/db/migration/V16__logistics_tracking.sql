ALTER TABLE order_fulfillments ADD COLUMN current_latitude DOUBLE PRECISION;
ALTER TABLE order_fulfillments ADD COLUMN current_longitude DOUBLE PRECISION;
ALTER TABLE order_fulfillments ADD COLUMN position_updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE order_fulfillments ADD COLUMN destination_warehouse_id BIGINT REFERENCES warehouses (id);
