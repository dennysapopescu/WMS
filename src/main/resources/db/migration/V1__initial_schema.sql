CREATE TABLE locations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    max_capacity INT NOT NULL,
    current_occupancy INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_location_capacity CHECK (max_capacity > 0),
    CONSTRAINT chk_location_occupancy CHECK (current_occupancy >= 0)
);

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    price DECIMAL(19,2) NOT NULL,
    location_id BIGINT,
    version BIGINT,
    CONSTRAINT chk_product_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_product_price CHECK (price >= 0),
    CONSTRAINT fk_product_location FOREIGN KEY (location_id) REFERENCES locations(id)
);
CREATE INDEX idx_products_location ON products(location_id);

CREATE TABLE outbound_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(100) NOT NULL,
    requested_quantity INT NOT NULL,
    picked_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    location_id BIGINT,
    assigned_to VARCHAR(100),
    assigned_at DATETIME,
    CONSTRAINT chk_order_quantities CHECK (requested_quantity > 0 AND picked_quantity >= 0 AND picked_quantity <= requested_quantity),
    CONSTRAINT fk_order_location FOREIGN KEY (location_id) REFERENCES locations(id)
);
CREATE INDEX idx_outbound_orders_status ON outbound_orders(status);

CREATE TABLE inventory_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    sku VARCHAR(100),
    action VARCHAR(80) NOT NULL,
    quantity_changed INT NOT NULL,
    timestamp DATETIME NOT NULL,
    performed_by VARCHAR(100) NOT NULL
);
CREATE INDEX idx_inventory_log_timestamp ON inventory_log(timestamp);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(30) NOT NULL,
    shift VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login DATETIME
);
