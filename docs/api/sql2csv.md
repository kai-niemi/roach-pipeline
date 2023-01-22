# sql2csv Resource

This resource represents a SQL to CSV pipeline job that will execute a series
of [keyset pagination](https://www.cockroachlabs.com/docs/stable/pagination.html#keyset-pagination)
queries against a source database and write the resultS to a CSV formatted
output. The CSV output stream can in turn be piped to CockroachDB's `IMPORT`
command, which would also be triggering the job via a `GET` request.

![cdc2sql](../images/cdc2sql.svg)

## Retrieve Form

A `GET` request will provide a form template for creating a job instance.
This form can optionally be pre-filled with SQL clauses and `UPSERT` statements
based on a source database introspection. To enable form template pre-fills, pass
in a `table` request parameter with the table name. See this
[guide](../forms.md) for more details.

### Response Structure

| Path               | Type   | Description                |
|--------------------|--------|----------------------------|
| _template.default  | Object | Form properties            |
| _links             | Object | Links to related resources |
| targetUrl          | String | *)                         |
| targetUsername     | String | *)                         |
| targetPassword     | String | *)                         |
| concurrency        | Number | *)                         |
| subscriberId       | String | *)                         | 
| chunkSize          | Number | *)                         |
| pollTimeoutSeconds | Number | *)                         |
| createQuery        | String | *)                         | 
| insertQuery        | String | *)                         | 

*) See `_template.default` properties for a description. This field
can be pre-populated from template settings or by database introspection.

### Example request

    curl --insecure -X GET https://localhost:8443/cdc2sql/form > cdc2sql.json

### Example response

See [cdc2sql.json](cdc2sql-req.json)

### Links

| Path                 | Description              |
|----------------------|--------------------------|
| pipeline:template | A template file resource |
| self                 | The cdc2sql resource     |
| curies               | Curies for documentation |

## Creating a job

A `POST` request will submit a job request.

### Example request

    curl https://localhost:8080/cdc2sql --insecure -i -X POST \
    -d '{
    "targetUrl" : "jdbc:postgresql://192.168.1.99:26300/tpcc_copy?sslmode=disable",
    "targetUsername" : "root",
    "targetPassword" : "",
    "concurrency" : 1,
    "subscriberId" : "193bf3dc-12d5-4cb8-a386-6733769cd981",
    "chunkSize" : 32,
    "pollTimeoutSeconds" : 25,
    "createQuery" : "CREATE TABLE IF NOT EXISTS products(id uuid not null default gen_random_uuid(), inventory int not null, name varchar(128) not null, price numeric(19, 2) not null, sku varchar(128) not null unique, primary key (id))",
    "insertQuery" : "UPSERT INTO products(id,inventory,name,price,sku) VALUES (:id,:inventory,:name,:price,:sku)"
    }'

or

    curl --insecure -d "@cdc2sql.json" -H "Content-Type:application/json" -X POST https://localhost:8443/cdc2sql

### Example response

    {
      "_links" : {
        "pipeline:cdc2sql-sink" : {
          "href" : "https://localhost:8443/cdc2sql/193bf3dc-12d5-4cb8-a386-6733769cd981",
          "title" : "cdc2sql sink endpoint"
        },
        "pipeline:execution" : {
          "href" : "https://localhost:8443/jobs/execution/future/f8f39a9e-d36c-4805-9893-7cb5214a3273",
          "title" : "job execution resource"
        },
        "curies" : [ {
          "href" : "https://localhost:8443/rels/{rel}",
          "name" : "pipeline",
          "templated" : true
        } ]
      },
      "message" : "cdc2sql job accepted for async processing",
      "createStatement" : "CREATE CHANGEFEED FOR TABLE null INTO 'webhook-https://localhost:8443/cdc2sql/193bf3dc-12d5-4cb8-a386-6733769cd981?insecure_tls_skip_verify=true' WITH updated, resolved='15s',  webhook_sink_config='{\"Flush\": {\"Messages\": 64, \"Frequency\": \"1s\"}, \"Retry\": {\"Max\": \"inf\"}}';",
      "_templates" : {
        "default" : {
          "method" : "POST",
          "contentType" : "*/*",
          "properties" : [ {
            "name" : "blank",
            "readOnly" : true
          }, {
            "name" : "bytes",
            "readOnly" : true
          }, {
            "name" : "empty",
            "readOnly" : true
          } ],
          "target" : "https://localhost:8443/cdc2sql/193bf3dc-12d5-4cb8-a386-6733769cd981"
        }
      }


## Remarks

For optimal performance, ensure the mandatory `sortKey` property includes all the
primary index columns in the exact same order (if not that will cause table scans).
