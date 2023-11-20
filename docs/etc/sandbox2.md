# DDL for PSQL

```sql
drop table transaction_item cascade ;
drop table transaction cascade ;
drop table account cascade ;

create table account
(
    id             int,
    balance        numeric(19, 2) not null,
    currency       varchar(64)    not null,
    name           varchar(128)   not null,

    primary key (id)
);

create table transaction
(
    id               int             not null,
    booking_date     date             null,
    primary key (id)
);

create table transaction_item
(
    transaction_id  int           not null,
    account_id      int           not null,
    amount          numeric(19, 2) not null,
    currency        varchar(64)    not null,
    running_balance numeric(19, 2) not null,
    note            varchar(255),

    primary key (transaction_id, account_id)
);

alter table transaction_item
    add constraint fk_txn_item_ref_transaction
        foreign key (transaction_id) references transaction (id);
alter table transaction_item
    add constraint fk_txn_item_ref_account
        foreign key (account_id) references account (id);
```

# DML for PSQL

```sql
insert into account (id,balance,currency,name)
select no,
       500.00 + random() * 500.00,
       'USD',
       md5(random()::text)
from generate_series(1, 10) no;

insert into transaction (id,booking_date)
select no,
       now()::date
from generate_series(1, 500000) no;

insert into transaction_item (transaction_id,account_id,amount,currency,running_balance,note)
select no,
       round(1 + random() * 9),
       500.00 + random() * 500.00,
       'USD',
       500.00 + random() * 500.00,
       'Cockroaches can eat anything'
from generate_series(1, 500000) no;
```

# Get and Post forms

    pg_dump --dbname=crdb_test --table=transaction_item --schema-only                                           
    curl -X GET http://localhost:8090/sql2sql/form?table=account > account.json
    curl -X GET http://localhost:8090/sql2sql/form?table=transaction > transaction.json
    curl -X GET http://localhost:8090/sql2sql/form?table=transaction_item > transaction_item.json
    curl -X GET http://localhost:8090/datasource/source-tables | grep topologyOrder
    curl -d "@account.json" -H "Content-Type:application/json" -X POST http://localhost:8090/sql2sql
    curl -d "@transaction.json" -H "Content-Type:application/json" -X POST http://localhost:8090/sql2sql
    curl -d "@transaction_item.json" -H "Content-Type:application/json" -X POST http://localhost:8090/sql2sql
                                                                                                          
