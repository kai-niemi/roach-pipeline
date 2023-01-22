-- CREATE extension pgcrypto;

DROP TABLE IF EXISTS products;

CREATE TABLE IF NOT EXISTS products
(
    id        uuid           not null default gen_random_uuid(),
    inventory int            not null,
    name      varchar(128)   not null,
    price     numeric(19, 2) not null,
    sku       varchar(128)   not null unique,
    primary key (id)
);

CREATE INDEX product_name_idx ON products (name);

-- INSERT INTO products (inventory,name,price,sku)
-- SELECT (random()*10)::int,
--        md5(random()::text),
--        (random()*150.00)::numeric,
--        md5(random()::text)
-- FROM generate_series(1, 500);

-- UPSERT INTO products(id,inventory,name,price,sku) VALUES (?,?,?,?,?);

-- select id,inventory,name,price,sku from products;