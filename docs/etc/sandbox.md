## SQL

```sql
SET CLUSTER SETTING kv.rangefeed.enabled = true;

SELECT job_id, description, num_runs, execution_errors
FROM [SHOW JOBS]
where job_type = 'CHANGEFEED'
and status = 'running';

CANCEL JOBS (SELECT job_id
FROM [SHOW JOBS]
where job_type = 'CHANGEFEED'
and status = 'running');

CREATE CHANGEFEED FOR TABLE t_account
INTO 'webhook-https://192.168.1.113:8443/cdc2sql/dd100e06-456f-4ce3-8bd3-a06a4dbc0ab5?insecure_tls_skip_verify=true'
WITH updated, resolved ='15s',
webhook_sink_config='{"Flush": {"Messages": 64, "Frequency": "1s"}, "Retry": {"Max": "inf"}}';

insert into t_account (id, balance)
select unordered_unique_rowid(),
500.00 + random() * 500.00
from generate_series(1, 10);

delete from t_account where 1=1 limit 5;
```

## CLI

```bash
curl -X GET http://localhost:8090/cdc2sql/form?table=t_account > form.json
curl -d "@form.json" -H "Content-Type:application/json" -X POST http://localhost:8090/cdc2sql
```
