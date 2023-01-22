# Stream a SQL query result to CockroachDB using UPSERT

In this tutorial we will pass a series of (keyset) pagination queries against PostgreSQL
and map the results to CockroachDB by using UPSERT statements.

Prerequisites:

- PostgresSQL database (the source)
- CockroachDB database (the target)

First create the source table in PostgreSQL and insert a few rows:
    
    CREATE TABLE customer
    (
        c_id           INT8           NOT NULL,
        c_d_id         INT8           NOT NULL,
        c_w_id         INT8           NOT NULL,
        c_first        VARCHAR(16)    NOT NULL,
        c_middle       CHAR(2)        NOT NULL,
        c_last         VARCHAR(16)    NOT NULL,
        c_street_1     VARCHAR(20)    NOT NULL,
        c_street_2     VARCHAR(20)    NOT NULL,
        c_city         VARCHAR(20)    NOT NULL,
        c_state        CHAR(2)        NOT NULL,
        c_zip          CHAR(9)        NOT NULL,
        c_phone        CHAR(16)       NOT NULL,
        c_since        TIMESTAMP      NOT NULL,
        c_credit       CHAR(2)        NOT NULL,
        c_credit_lim   DECIMAL(12, 2) NOT NULL,
        c_discount     DECIMAL(4, 4)  NOT NULL,
        c_balance      DECIMAL(12, 2) NOT NULL,
        c_ytd_payment  DECIMAL(12, 2) NOT NULL,
        c_payment_cnt  INT8           NOT NULL,
        c_delivery_cnt INT8           NOT NULL,
        c_data         VARCHAR(500)   NOT NULL,
    
        CONSTRAINT "primary" PRIMARY KEY (c_w_id, c_d_id, c_id)
    );
    
    CREATE INDEX customer_idx ON customer (c_w_id , c_d_id , c_last , c_first);
   
    INSERT INTO customer(c_id,c_d_id,c_w_id,c_first,c_middle,c_last,c_street_1,c_street_2,c_city,c_state,c_zip,c_phone,c_since,c_credit,c_credit_lim,c_discount,c_balance,c_ytd_payment,c_payment_cnt,c_delivery_cnt,c_data)
    VALUES
    (2001,7,6,'rumMmp6NHnwiwKd','OE','PRIOUGHTEING','cgphy3v1U5yraPxxELo','5B1fcW8RsaCXoEz','mssaF9m9cdLXe0Y','UT',230811111,2947329423220144,'2006-01-02 15:04:05','GC',50000.00,0.3181,-10.00,10.00,1,0,'hgLRrwsmd68P2bElAgrnp8ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nWB1rXYANQ0fa6ZQJJI2uTeFFazaVwxnN13XunKGV6AwCKxhJQVgXWaljKLJ7r175FAuGYFLyxJvnAUXEp2watyJTTtfENexKnKSQN6vWniabVBVqad2oZO92wV1AnAKYTj7QrlNH'),
    (2002,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW'),
    (2003,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW'),
    (2004,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW'),
    (2005,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW'),
    (2006,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW'),
    (2007,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW'),
    (2008,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW'),
    (2009,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW'),
    (2010,7,6,'yraPxxELo5B1fcW','OE','PRIESEOUGHT','8RsaCXoEzmssaF9','m9cdLXe0YhgLRr','wsmd68P2bElAgrnp8','SF',082911111,4732942322014469,'2006-01-02 15:04:05','GC',50000.00,0.2492,-10.00,10.00,1,0,'ueWNXJpBB0ObpVWo1BahdejZrKB2O3Hzk13xWSP8P9fwb2ZjtZAs3NbYdihFxFime6B6Adnt5jrXvRR7OGYhlpdljbDvShaRF4E9zNHsJ7ZvyiJ3n2X1f4fJoMgn5buTDyUmQupcYMoPylHqYo89SqHqQ4HFVNpmnIWHyIowzQN2r4uSQJ8PYVLLLZk9Epp6cNEnaVrN3JXcrBCOuRRSlC0zvh9lctkhRvAvE5H6TtiDNPEJrcjAUOegvQ1Ol7SuF7jPf275wNDlEbdC58hrunlPfhoY1dORoIgb0VnxqkqbEWTXujHUOvCRfqCdVyc8gRGMfAd4nW')
    ;

Next, call the REST endpoint to get a form template to describe the job:

    curl -X GET http://localhost:8090/sql2sql/form > customers-job.json                                           

The output looks like this after filtering all '_' prefixes:

    {
        "sourceUrl" : "jdbc:postgresql://localhost:5432/tpcc_copy",
        "sourceUsername" : "postgres",
        "sourcePassword" : "",
        "sortKeys" : "c_w_id ASC, c_d_id ASC, c_id ASC",
        "selectClause" : "SELECT *",
        "fromClause" : "FROM customer",
        "whereClause" : "WHERE 1=1",
        "chunkSize" : 256,
        "linesToSkip" : 0,
        "pageSize" : 32,
        "targetUrl" : "jdbc:postgresql://localhost:26257/tpcc_copy",
        "targetUsername" : "postgres",
        "targetPassword" : "",
        "createQuery" : "CREATE TABLE IF NOT EXISTS customer\n(\n    c_id           INT8           NOT NULL,\n    c_d_id         INT8           NOT NULL,\n    c_w_id         INT8           NOT NULL,\n    c_first        VARCHAR(16)    NOT NULL,\n    c_middle       CHAR(2)        NOT NULL,\n    c_last         VARCHAR(16)    NOT NULL,\n    c_street_1     VARCHAR(20)    NOT NULL,\n    c_street_2     VARCHAR(20)    NOT NULL,\n    c_city         VARCHAR(20)    NOT NULL,\n    c_state        CHAR(2)        NOT NULL,\n    c_zip          CHAR(9)        NOT NULL,\n    c_phone        CHAR(16)       NOT NULL,\n    c_since        TIMESTAMP      NOT NULL,\n    c_credit       CHAR(2)        NOT NULL,\n    c_credit_lim   DECIMAL(12, 2) NOT NULL,\n    c_discount     DECIMAL(4, 4)  NOT NULL,\n    c_balance      DECIMAL(12, 2) NOT NULL,\n    c_ytd_payment  DECIMAL(12, 2) NOT NULL,\n    c_payment_cnt  INT8           NOT NULL,\n    c_delivery_cnt INT8           NOT NULL,\n    c_data         VARCHAR(500)   NOT NULL,\n    CONSTRAINT \"primary\" PRIMARY KEY (c_w_id ASC, c_d_id ASC, c_id ASC),\n    INDEX          customer_idx(c_w_id ASC, c_d_id ASC, c_last ASC, c_first ASC)\n);",
        "insertQuery" : "UPSERT INTO customer(c_id, c_d_id, c_w_id, c_first, c_middle, c_last, c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, c_since, c_credit, c_credit_lim, c_discount, c_balance, c_ytd_payment, c_payment_cnt, c_delivery_cnt, c_data) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
    }

Fields:

- `sortKeys` - Enables keyset pagination where its important to set this correctly (same order as in the index)
  for large tables to avoid full scans when iterating pages.
- `selectClause` - Mandatory, can use projection
- `fromClause` - Mandatory
- `whereClause` - Optional
- `chunkSize` - the batch size of inserts/upserts
- `linesToSkip` - Offset to start from when resuming from errors
- `pageSize` - the pagination query size
- `createQuery` - Use this or createScript which points to a DDL file
- `insertQuery` - See above

Modify the connection parameters accordingly and POST it back using cURL:

    curl -d "@customers-job.json" -H "Content-Type:application/json" -X POST http://localhost:8090/sql2sql
                                                                                                          
    "_links" : {
        "pipeline:execution" : {
          "href" : "http://localhost:8090/jobs/execution/future/6fd3bcd8-da30-4f3e-bb29-976265e6eccd"
        },
        "curies" : [ {
          "href" : "http://localhost:8090/rels/{rel}",
          "name" : "pipeline",
          "templated" : true
        } ]
      },
      "message" : "sql2sql job accepted for async processing"
    }

If you get a 201 status code the job was submitted successfully for processing. To inspect the job status
and potential errors, follow the link provided in the response:

    curl -X GET http://localhost:8090/jobs/execution/future/6fd3bcd8-da30-4f3e-bb29-976265e6eccd

Look for:

    "batchStatus" : "COMPLETED",
    "exitStatus" : {
        "exitCode" : "COMPLETED",
        "exitDescription" : "",
        "running" : false
    }

Verify that the rows were inserted in CockroachDB:

    select * from customer;
