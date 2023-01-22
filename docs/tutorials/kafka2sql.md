# Stream a Kafka changefeed from CockroachDB to CockroachDB using UPSERT

In this tutorial, we will subscribe to a Kafka changefeed topic and write the events
to a target database using UPSERTs. The target is another CockroachDB database and 
the source topic is populated using the CockroachDB's Kafka CDC sink. Effectively 
cluster to cluster streaming, scoped by table.

First, create the following table both in the source and target databases:

    CREATE TABLE products
    (
        id        uuid           not null default gen_random_uuid(),
        inventory int            not null,
        name      varchar(128)   not null,
        price     numeric(19, 2) not null,
        sku       varchar(128)   not null unique,
        primary key (id)
    );

Next, load the source database with a few rows:

    INSERT INTO products (inventory,name,price,sku)
        SELECT random()*10,
            md5(random()::text),
            random()*150.00,
            md5(random()::text)
        FROM generate_series(1, 100);

We are now ready to register a batch job for the changefeed pipeline.
Assuming the batch service is already started, let's begin by getting 
a form template that describes the job:

    curl --insecure -X GET https://localhost:8443/kafka2sql/form > kafka-job.json                                           

Edit the `kafka-job.json` form accordingly to your environment:
                
    {
      "name" : "products-reader",
      "topic" : "products",
      "bootstrapServers" : "localhost:9092",
      "chunkSize" : 10,
      "pollTimeoutSeconds" : 300,
      "targetUrl" : "jdbc:postgresql://localhost:26257/other_db",
      "targetUsername" : "root",
      "targetPassword" : "",
      "createQuery" : "",
      "insertQuery" : "UPSERT INTO products(id,inventory,name,price,sku) VALUES (:id,:inventory,:name,:price,:sku)"
    }

(All elements prefixed with `_` are removed for readability) 

A few things to notice about the parameters:

- name - Can be anything 
- topic - The Kafka topic to connect the consumer to (typically the table name)
- bootstrapServers - Kafka bootstrap servers list
- chunkSize - Defines the number of CDC events to read before writing which 
translates to the batch statement size. 
- pollTimeoutSeconds - Time to wait for events until quitting (job completion)
- createQuery - DDL statement or file URL (can be empty)
- insertQuery - The query used to INSERT data using named parameters. The parameter names must match the field names in the changefeeds (column names).

Assuming Kafka cluster is available, lets POST the form back to create the job:

    curl --insecure -d "@kafka-job.json" -H "Content-Type:application/json" -X POST https://localhost:8443/kafka2sql

If you get a `201` status code then the job was submitted successfully for processing. 

For example:
      
    "_links" : {
        "pipeline:execution" : {
          "href" : "https://localhost:8443/jobs/execution/future/04a004ab-8905-4b64-928d-47d2e4894ca2"
        },
        "curies" : [ {
          "href" : "https://localhost:8443/rels/{rel}",
          "name" : "pipeline",
          "templated" : true
        } ]
      },
      "message" : "kafka2sql job accepted for async processing"
    }

To inspect the job status for potential errors, follow the link rel `pipeline:execution` provided in the response:

    curl --insecure -X GET https://localhost:8443/jobs/execution/future/04a004ab-8905-4b64-928d-47d2e4894ca2
    
Next, create the change feed in the source database (updated,resolved are optional):

    SET CLUSTER SETTING kv.rangefeed.enabled = true;

    CREATE CHANGEFEED FOR TABLE products
        INTO 'kafka://localhost:9092' WITH updated,resolved = '15s';

To inspect the job status for potential errors, follow the link rel `pipeline:execution` provided in the response:

    curl --insecure -X GET https://localhost:8443/jobs/execution/future/04a004ab-8905-4b64-928d-47d2e4894ca2

It should instantly start to populate the target database with 100 products.

Next, lets load a few more rows in the source database:

    INSERT INTO products (inventory,name,price,sku)
        SELECT random()*10,
            md5(random()::text),
            random()*150.00,
            md5(random()::text)
        FROM generate_series(1, 500);

The job will end when there's nothing received from the Kafka topic and the timeout expires (default is 5min).

Look for COMPLETED status:

    curl --insecure -X GET https://localhost:8443/jobs/execution/future/04a004ab-8905-4b64-928d-47d2e4894ca2

    ...

    "batchStatus" : "COMPLETED",
    "exitStatus" : {
        "exitCode" : "COMPLETED",
        "exitDescription" : "",
        "running" : false
    }

Finally, verify that you have the same number of rows in both databases:

    select count(1) from products;

If the job expires or there's an error, you can always create new job by posting 
the same form as pointed out above. 
