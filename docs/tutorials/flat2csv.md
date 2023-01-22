# Import a flat file from S3 to CockroachDB using IMPORT INTO

In this tutorial we will read a fixed-width flat file in S3 and map it
to CSV format consumed by `IMPORT INTO`. Effectively it's a direct
streaming pipeline to CockroachDB from a file in S3, only its passed
through pipeline to convert it from fixed-width to CSV.

Prerequisites:

- S3 bucket (the source)
- CockroachDB database (the target)

Assume a fixed width flat file in S3 containing purchase orders:

    UK21341EAH4121131.11customer1
    UK21341EAH4221232.11customer2
    UK21341EAH4321333.11customer3
    UK21341EAH4421434.11customer4

The layout of this file is described with a JSON document, also stored alongside the flat file in S3:

    {
        "fields": [
            {
                "name": "SKU",
                "range": {
                    "min": 1,
                    "max": 12
                }
            },
            {
                "name": "Quantity",
                "range": {
                    "min": 13,
                    "max": 15
                }
            },
            {
                "name": "Price",
                "range": {
                    "min": 16,
                    "max": 20
                }
            },
            {
                "name": "Total",
                "expression": "#fieldSet.readLong(\"Price\") * #fieldSet.readLong(\"Quantity\")"
            },
            {
                "name": "Customer",
                "range": {
                    "min": 21,
                    "max": 29
                }
            }
        ],
        "tokenizer": {
            "type": "fixed",
            "strict": true
        }
    }


Fist make a test request to convert the flat file to CSV:

    curl --location --request GET 'http://localhost:8090/flat2csv?source=s3://bucket-name/test.txt&schema=s3://bucket-name/test.json&AWS_ACCESS_KEY_ID=<key>&AWS_SECRET_ACCESS_KEY=<key>&AWS_DEFAULT_REGION=eu-central-1'

Create the table:

    create table orders
    (
        sku      string      not null,
        qty      string      not null,
        price    string      not null,
        total    string      not null,
        customer string      not null
    );

Now we can execute the `IMPORT INTO` command by pointing at S3 file and json schema:

    import into orders(sku,qty,price,total,customer)
        CSV DATA (
            'http://localhost:8090/flat2csv?source=s3://bucket-name/test.txt&schema=s3://bucket-name/test.json&AWS_ACCESS_KEY_ID=<key>&AWS_SECRET_ACCESS_KEY=<key>&AWS_DEFAULT_REGION=<region>'
    );

