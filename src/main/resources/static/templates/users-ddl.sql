-- DROP TABLE users;
CREATE TABLE IF NOT EXISTS users
(
    id         UUID   NOT NULL DEFAULT gen_random_uuid(),
    city       STRING NOT NULL,
    first_name STRING NOT NULL,
    last_name  STRING NOT NULL,
    full_name  STRING AS (CONCAT(first_name, ' ', last_name)) STORED,
    fav_color  STRING NULL     default 'n/a',
    profile    JSONB  NULL,

    PRIMARY KEY (city ASC, id ASC)
);

-- UPSERT INTO users(id, city, first_name, last_name, fav_color, profile) values (?,?,?,?,?,?)

COMMENT ON COLUMN users.id IS 'selectOne(''select gen_random_uuid()'')';
COMMENT ON COLUMN users.city IS 'randomCity()';
COMMENT ON COLUMN users.first_name IS 'randomFirstName()';
COMMENT ON COLUMN users.last_name IS 'randomLastName()';
COMMENT ON COLUMN users.fav_color IS 'if rowNumber() % 2 == 0 then ''red'' otherwise ''green''';
COMMENT ON COLUMN users.profile IS 'randomJson(5,2)';

-- COMMENT ON COLUMN users.id IS NULL;
-- COMMENT ON COLUMN users.city IS NULL;
-- COMMENT ON COLUMN users.first_name IS NULL;
-- COMMENT ON COLUMN users.last_name IS NULL;
-- COMMENT ON COLUMN users.fav_color IS NULL;
-- COMMENT ON COLUMN users.profile IS NULL;

SHOW COLUMNS FROM users WITH COMMENT;

-- insert into users (city, first_name, last_name)
-- select 'stockholm',
--        md5(random()::text),
--        md5(random()::text)
-- from generate_series(1, 1000);
-- select full_name
-- from users
-- order by id
-- offset 0 limit 1;

