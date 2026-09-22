CREATE TABLE IF NOT EXISTS events (
 id VARCHAR(64) PRIMARY KEY, title TEXT NOT NULL, type VARCHAR(64) NOT NULL,
 date_time TIMESTAMP NOT NULL, hall TEXT NOT NULL, total_seats INTEGER NOT NULL CHECK(total_seats>0),
 free_seats INTEGER NOT NULL CHECK(free_seats>=0), price NUMERIC(12,2) NOT NULL CHECK(price>=0),
 status VARCHAR(32) NOT NULL, description TEXT, version BIGINT NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS orders (
 id VARCHAR(64) PRIMARY KEY, event_id VARCHAR(64) NOT NULL REFERENCES events(id),
 reader_card TEXT NOT NULL, reader_name TEXT NOT NULL, seats INTEGER NOT NULL CHECK(seats>0),
 total_price NUMERIC(12,2) NOT NULL CHECK(total_price>=0), status VARCHAR(32) NOT NULL,
 manager_login VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_orders_event ON orders(event_id);