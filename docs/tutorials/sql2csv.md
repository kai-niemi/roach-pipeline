# Stream a SQL query result to CockroachDB using IMPORT INTO

In this tutorial, we will pass a series of (keyset) pagination queries against PostgreSQL, 
map the result to CSV and then stream it over HTTP to CockroachDB by using IMPORT INTO. 
The IMPORT INTO command will be the actual job trigger.

Prerequisites:
 
- PostgresSQL database named `tpcc`
- CockroachDB database named `tpcc_copy`  

First create the source database in PostgreSQL:
                                 
    postgres=# create database tpcc;  
    postgres=# \c tpcc  

Then create the source table and generate a few rows:

    CREATE extension pgcrypto;

    CREATE TABLE products
    (
        id        uuid           not null default gen_random_uuid(),
        inventory int            not null,
        name      varchar(128)   not null,
        price     numeric(19, 2) not null,
        sku       varchar(128)   not null unique,
        primary key (id)
    );

    INSERT INTO products (inventory,name,price,sku)
        SELECT random()*10,
        md5(random()::text),
        random()*150.00,
        md5(random()::text)
    FROM generate_series(1, 10000);

Next, call the REST endpoint to get a form template to describe the job:

    curl -X GET http://localhost:8090/sql2csv/form > products-job.json
    cat products-job.json                                                                        
                                                                    
The output looks something like this (filtering the '_' prefixed elements for readability):

    {
        "sourceUrl" : "jdbc:postgresql://localhost:5432/tpcc",
        "sourceUsername" : "postgres",
        "sourcePassword" : "",
        "sortKeys" : "id",
        "selectClause" : "SELECT id,inventory,name,price,sku",
        "fromClause" : "FROM products",
        "whereClause" : "WHERE 1=1",
        "chunkSize" : 256,
        "linesToSkip" : 0,
        "pageSize" : 32,
    }

Fields:

- sourceUrl - JDBC url to source DB 
- sourceUsername - JDBC user name 
- sourcePassword - JDBC user password 
- sortKeys - Comma separated list of sort keys used for keyset pagination. 
It's important to set these in the same order in the primary index to avoid full scans.
- selectClause - Mandatory, must use named parameters and projection
- fromClause - Mandatory, typically single table but joins are possible also
- whereClause - Optional, predicate for source query
- chunkSize - number of rows (items) to read before a batch write. 
Hint: start low and use a multiples of 16 
- linesToSkip - Offset to start from when resuming from errors
- pageSize - the pagination query size on the read side

After modifying the parameters above to match your setup, then POST the form back to the `target` 
URI defined in `_templates` using cURL (bottom of the json doc). It should print the SQL query 
results in CSV format to stdout.

    curl -d "@products-job.json" -H "Content-Type:application/json" -X POST http://localhost:8090/sql2csv

Now, we will do the same thing but use `IMPORT INTO` in CockroachDB to ingest the CSV data. 

Connect to the target CockroachDB database and create the table:

    $ cockroach sql --insecure --host localhost:26257 --database tpcc_copy

    CREATE TABLE products
    (
        id        uuid           not null default gen_random_uuid(),
        inventory int            not null,
        name      varchar(128)   not null,
        price     numeric(19, 2) not null,
        sku       varchar(128)   not null unique,
        primary key (id)
    );

Now execute `IMPORT INTO` with a parameter pointing at the local json form:

    IMPORT INTO products(id,inventory,name,price,sku)
    CSV DATA (
        'http://localhost:8090/sql2csv?params=nodelocal:products-job.json'
    );
            job_id       |  status   | fraction_completed | rows  | index_entries |  bytes
    ---------------------+-----------+--------------------+-------+---------------+----------
      751852029880795137 | succeeded |                  1 | 10000 |         10000 | 1859941
    (1 row)                                                                                

    select count(1) from products;
    count
    ---------
    10000
    (1 row)

Now you have effectively streamed the contents of the `products` table in PostgreSQL to 
CockroachDB using a set of keyset pagination queries.

For more details on IMPORTs, see [Import into a new table from a CSV file](https://www.cockroachlabs.com/docs/v21.2/import-into#import-into-a-new-table-from-a-csv-file).
                  
