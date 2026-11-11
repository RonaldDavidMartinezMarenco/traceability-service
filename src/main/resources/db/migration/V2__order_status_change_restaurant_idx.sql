CREATE INDEX order_status_change_restaurant_status_idx
    ON order_status_change (restaurant_id, new_status, order_id);
