CREATE TYPE order_status AS ENUM ('NEW', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED');

CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    city VARCHAR(100) NOT NULL,
    street VARCHAR(100) NOT NULL,
    house_number VARCHAR(10) NOT NULL,
    postal_code VARCHAR(20)
);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    date_of_birth DATE CHECK (date_of_birth < CURRENT_DATE),
    phone_number VARCHAR(20),
    email VARCHAR(50),
    role VARCHAR(100) NOT NULL,
    address_id BIGINT REFERENCES addresses(id) ON DELETE SET NULL
);

CREATE TABLE pickup_locations (
    id BIGSERIAL PRIMARY KEY,
    city VARCHAR(100) NOT NULL,
    street VARCHAR(100) NOT NULL,
    house_number VARCHAR(10) NOT NULL
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    status order_status NOT NULL,
    customer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    pickup_location_id BIGINT REFERENCES pickup_locations(id) ON DELETE SET NULL,
    address_id BIGINT REFERENCES addresses(id) ON DELETE SET NULL
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL UNIQUE,
    product_description VARCHAR(600) NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    product_category VARCHAR(100) NOT NULL,
    availability BOOLEAN NOT NULL,
    order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL
);
