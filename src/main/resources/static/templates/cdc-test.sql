SET CLUSTER SETTING kv.rangefeed.enabled = true;

CREATE CHANGEFEED FOR TABLE products
    INTO 'kafka://localhost:9092' WITH updated, resolved = '15s';
CREATE CHANGEFEED FOR TABLE products
    INTO 'kafka://192.168.1.99:9092' WITH updated, resolved = '15s';

CREATE CHANGEFEED FOR TABLE products
    INTO 'webhook-https://192.168.1.145:8443/cdc2sql/446bf3a6-05d1-40c4-89ca-612af4099241?insecure_tls_skip_verify=true'
    WITH updated, resolved ='30s',
    webhook_sink_config='{"Flush": {"Messages": 15, "Frequency": "1s"}, "Retry": {"Max": "inf"}}';

CREATE CHANGEFEED FOR TABLE products INTO
'webhook-https://192.168.1.145:8443/cdc2sql/test?insecure_tls_skip_verify=true'
WITH updated, resolved='15s',  webhook_sink_config='{"Flush": {"Messages": 32, "Frequency": "1s"}, "Retry": {"Max": "inf"}}';

INSERT INTO products (inventory, name, price, sku)
SELECT (random() * 10)::int,
       md5(random()::text),
       (random() * 150.00)::decimal,
       md5(random()::text)
FROM generate_series(1, 100);

-- delete from products where id='18955dc6-400d-4bb9-96c0-125bbe95e4ab';

CANCEL JOBS (SELECT job_id
             FROM [SHOW JOBS]
             where job_type = 'CHANGEFEED');

/*
{
  "key" : [ "974e01ca-37ee-4207-92e5-e15f6dac1853" ],
  "topic" : "products",
  "updated" : "1652170737058712382.0000000000",
  "after" : {
    "id" : "974e01ca-37ee-4207-92e5-e15f6dac1853",
    "inventory" : "4",
    "name" : "6068854d611e96c4253259b5da750081",
    "price" : "84.55",
    "sku" : "69b471cc98ce62832d538ab4919f2149"
  }
}
*/
