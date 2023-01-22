# TPC-C via CDC Kafka Sink


## Prerequisites

First you need the following databases:

```bash
cockroach sql --insecure --host=localhost -e "CREATE database tpcc"
cockroach sql --insecure --host=localhost -e "CREATE database tpcc_copy"
```

Then load a TPC-C test fixture to the `tpcc` database:

    cockroach workload fixtures import tpcc --warehouses=100 'postgres://root@localhost:26257?sslmode=disable'

## Generate Form Templates

The following commands will download form templates (in JSON) that are pre-populated with necessary SQL statements. We'll just use a subset of the TPC-C tables, but the process is the same for the others.

```bash
curl -X GET http://localhost:8090/kafka2sql/form?table=warehouse > warehouse-kafka2sql.json
curl -X GET http://localhost:8090/kafka2sql/form?table=district > district-kafka2sql.json
curl -X GET http://localhost:8090/kafka2sql/form?table=customer > customer-kafka2sql.json
```

## Submit Batch Jobs

The JSON files typically don't need any editing if the template settings are correct. The next step is just to POST them back which will register the jobs.
For the time being, these need to be registered in the sorted topology order of the foreign key constraints `(warehouse <- district <-customer)`.

```bash
curl -d "@warehouse-kafka2sql.json" -H "Content-Type:application/json" -X POST http://localhost:8090/kafka2sql
curl -d "@district-kafka2sql.json" -H "Content-Type:application/json" -X POST http://localhost:8090/kafka2sql
curl -d "@customer-kafka2sql.json" -H "Content-Type:application/json" -X POST http://localhost:8090/kafka2sql
```

The final step is to configure the Kafka change feeds for these three tables.

Connect to the source database and execute:

```bash
CREATE CHANGEFEED FOR TABLE warehouse INTO 'kafka://192.168.1.99:9092' WITH updated,resolved = '15s';
CREATE CHANGEFEED FOR TABLE district INTO 'kafka://192.168.1.99:9092' WITH updated,resolved = '15s';
CREATE CHANGEFEED FOR TABLE customer INTO 'kafka://192.168.1.99:9092' WITH updated,resolved = '15s';
```


