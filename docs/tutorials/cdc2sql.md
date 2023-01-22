# Stream a HTTP changefeed from CockroachDB to CockroachDB using UPSERT

In this tutorial, we will receive changefeeds via the HTTP sink from a source CockroachDB and 
map these to batch UPSERTs against a target CockroachDB. Effectively cluster-to-cluster
streaming via CDC, per table level.

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

    curl --insecure -X GET https://localhost:8443/cdc2sql/form > cdc-job.json                                           

Edit the `cdc-job.json` form and fill in the details:
                     
    {
      "subcriberId" : "3a3519fc-42be-4d7a-a67f-f37e342df49c",
      "chunkSize" : 10,
      "targetUrl" : "jdbc:postgresql://localhost:26257/other_db",
      "targetUsername" : "root",
      "targetPassword" : "",
      "createQuery" : "",
      "insertQuery" : "UPSERT INTO products(id,inventory,name,price,sku) VALUES (:id,:inventory,:name,:price,:sku)",
    }

(All elements prefixed with `_` are removed for readability)

A few things to notice about the parameters:
 
`` - subscriberId - Used to hook the job together with the CDC changefeed (this ID is opaque and doesnt need to be UUID)
 - chunkSize - Defines number of CDC `payload` items read at minimum before writing. 
This also translates to the batch statement size. 
**Take note that there are no durability guarantees for the events queued up.** 
`` - createQuery - DDL statement or file URL (can be empty)
 - insertQuery - The query used to INSERT data using named parameters. The parameter names must match table column names.

Now POST the form back:

    curl --insecure -d "@cdc-job.json" -H "Content-Type:application/json" -X POST https://localhost:8443/cdc2sql

If you get a 201 status code the job was submitted successfully for processing. To inspect the job status
and potential errors, follow the link rel `pipeline:execution`provided in the response. 

For example:

    "_links" : {
        "pipeline:cdc2sql-sink" : {
          "href" : "https://localhost:8443/cdc2sql/51b8cd5a-5304-4e7a-942a-4769e3b38f7e",
          "title" : "CDC webhook sink URI"
        },
        "pipeline:execution" : {
          "href" : "https://localhost:8443/jobs/execution/future/51b8cd5a-5304-4e7a-942a-4769e3b38f7e"
        },
        "curies" : [ {
          "href" : "https://localhost:8443/rels/{rel}",
          "name" : "pipeline",
          "templated" : true
        } ]
      },
      "message" : "cdc2sql job accepted for async processing"
    }

Take note of the HREF for the link rel `pipeline:cdc2sql-sink``. This is the URL you need to use 
when creating the CHANGEFEED in CockroachDB.

Next, create the webhook change feed against that endpoint (the sink config is optional):

    SET CLUSTER SETTING kv.rangefeed.enabled = true;

    CREATE CHANGEFEED FOR TABLE products
        INTO 'webhook-https://localhost:8443/cdc2sql/51b8cd5a-5304-4e7a-942a-4769e3b38f7e?insecure_tls_skip_verify=true'
        WITH updated, resolved='15s',
            webhook_sink_config='{"Flush": {"Messages": 5, "Frequency": "1s"}, "Retry": {"Max": "inf"}}';


Again, to inspect the job status for potential errors, follow the link rel `pipeline:execution` 
provided previously:

    curl --insecure -X GET https://localhost:8443/jobs/execution/future/51b8cd5a-5304-4e7a-942a-4769e3b38f7e

Look for COMPLETED status:

    "batchStatus" : "COMPLETED",
    "exitStatus" : {
        "exitCode" : "COMPLETED",
        "exitDescription" : "",
        "running" : false
    }

Finally, verify that the rows were inserted in the target CockroachDB:

    select count(1) from products;

The job will end when there's nothing received from the CDC endpoint 
and the timeout expires (default is 5min).

Done (phew)!

If the job expires or there's an error, you can create new job with 
the same form as pointed out above. 
