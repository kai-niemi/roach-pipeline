# Tutorials

Tutorials for different mapping commands.

- [kafka2sql and TPC-C](kafka2sql-tpcc.md) - Stream tables from the TPC-C workload fixture using kafka2sql
- [cdc2sql and TPC-C](cdc2sql-tpcc.md) - Stream tables from the TPC-C workload fixture using cdc2sql
- [kafka2sql](kafka2sql.md) - Stream a Kafka changefeed from CockroachDB to CockroachDB using UPSERT
- [cdc2sql](cdc2sql.md) - Stream a HTTP changefeed from CockroachDB to CockroachDB using UPSERT
- [sql2csv](sql2csv.md) - Stream a SQL query result to CockroachDB using IMPORT INTO
- [sql2sql](sql2sql.md) - Stream a SQL query result to CockroachDB using UPSERT
- [csv2sql](csv2sql.md) - Import a CSV file to CockroachDB using UPSERT
- [flat2csv](flat2csv.md) - Import a flat file from S3 to CockroachDB using IMPORT INTO 

## Pre-filled Form Templates  

In these tutorials there is an option to use pre-filled form templates with generated 
SQL create, delete and upsert commands based on source database introspection. 
This will significantly reduce the effort to manually craft all CREATE, INSERT, 
IMPORT, CREATE CHANGEFEED commands.

To enable pre-filling, you need to configure a source and target template database
connection at startup time. 

The source database is used to generate the pre-filled SQL statements. 
The target database connection parameters are passed into the templates.
                                                      
Example using a PSQL source DB and CRDB target DB:

    java -jar pipeline.jar \
    --spring.datasource.url=jdbc:postgresql://192.168.1.2:26257/pipeline?sslmode=disable \
    --pipeline.template.source.url=jdbc:postgresql://192.168.1.2:5432/tpcc \
    --pipeline.template.source.username=postgres \
    --pipeline.template.source.password=root \
    --pipeline.template.target.url=jdbc:postgresql://192.168.1.2:26257/tpcc_copy?sslmode=disable \
    --pipeline.template.target.username=root \
    --pipeline.template.target.password=
    $*

To leverage templating just add a `table` query parameter when retrieving a form template:

    curl -X GET http://localhost:8090/cdc2sql/form?table=products
   
To download pre-filled form templates for _all_ tables in a source database, use:

    curl -X GET http://localhost:8090/cdc2sql/forms/bundle -L -o bundle.zip

 