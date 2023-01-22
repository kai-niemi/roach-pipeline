# TPC-C via CDC Webhook Sink

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
curl -X GET http://localhost:8090/cdc2sql/form?table=warehouse > warehouse-cdc2sql.json
curl -X GET http://localhost:8090/cdc2sql/form?table=district > district-cdc2sql.json
curl -X GET http://localhost:8090/cdc2sql/form?table=customer > customer-cdc2sql.json
```

## Submit Batch Jobs

The JSON files typically don't need any editing if the template settings are correct. The next step is just to POST them back which will register the jobs.
For the time being, these need to be registered in the sorted topology order of the foreign key constraints `(warehouse <- district <-customer)`.

```bash
curl -d "@warehouse-cdc2sql.json" -H "Content-Type:application/json" -X POST http://localhost:8090/cdc2sql
curl -d "@district-cdc2sql.json" -H "Content-Type:application/json" -X POST http://localhost:8090/cdc2sql
curl -d "@customer-cdc2sql.json" -H "Content-Type:application/json" -X POST http://localhost:8090/cdc2sql
```

The final step is to configure the change feeds for these three tables.

Connect to the source database and execute (after changing URIs):

```bash
CREATE CHANGEFEED FOR TABLE warehouse 
    INTO 'webhook-https://localhost:8443/cdc2sql/5803c5a2-707a-4fb1-8faf-615d95896664?insecure_tls_skip_verify=true' 
    WITH updated, resolved='15s';

CREATE CHANGEFEED FOR TABLE district 
    INTO 'webhook-https://localhost:8443/cdc2sql/5803c5a2-707a-4fb1-8faf-615d95896664?insecure_tls_skip_verify=true' 
    WITH updated, resolved='15s';

CREATE CHANGEFEED FOR TABLE customer 
    INTO 'webhook-https://localhost:8443/cdc2sql/5803c5a2-707a-4fb1-8faf-615d95896664?insecure_tls_skip_verify=true' 
    WITH updated, resolved='15s';
```

