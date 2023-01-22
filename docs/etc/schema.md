# Source File Layout Schema

The layout of CSV and fixed-width flat files is described using a JSON document, called 
the source file layout schema. 

The schema structure is fairly self-explanatory: 
 
- Each field is mapped to a column in the intended output whether that's another CSV file or a table column.
- Fixed-length flat files use range descriptors for fields
- The tokenizer can either be `fixed`, `delimited` or use a `regexp` pattern for filtering out data in rows. 
- Columns can have SpEL expressions that can reference values in the current line being processed or use any 
- other computation result.

## Layout Fields

* `name` - (optional) used for documentation
* `strict` - source input file must exist or an exception is thrown (otherwise log warning)
* `comments` - collection of comment line prefixes
* `fields` - collection of fields to map
    * `name` - column name
    * `range` - field position with start and end range
      * `min` - start column index (1-based, inclusive) for fixed-length tokenizer
      * `max` - end column index (inclusive) for fixed-length tokenizer
    * `expression` - Defines a formatting expression based on [SpEL](https://docs.spring.io/spring-framework/docs/4.3.12.RELEASE/spring-framework-reference/html/expressions.html).
      The expression must resolve to a string and the following object
      references are pre-set:
        * `_fieldSet_`
          represents the [fieldset](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/item/file/transform/FieldSet.html) of the current line that can be referenced in expressions.
        * The `_names_`
          represents an array of the field names
        * The `_values_`
          represents an array of the current line field values
* `tokenizer` - attributes for the tokenizer
    * `type` - `fixed|delimited|regex`, default is `delimited`
    * `strict` - less lenient to errors, default is `false`
    * `pattern` - for regex mode, a regular expression to match lines to import, default is none
    * `quoteCharacter` - for delimited mode, default is `"`
    * `delimiter` - for delimited mode, default is `;`
    
# CSV Example 

## Source file

    id,firstName,lastName,zipcode,phone,email
    1,Deanna,Melanie,123 45,000-0000000,dm@mail.com
    2,Booker,Hudson,123 45,000-0000000,bh@mail.com
    3,Adriana,Alana,123 45,000-0000000,aa@mail.com
    4,Brande,Kelli,123 45,000-0000000,bk@mail.com
    5,Beatrice,India,123 45,000-0000000,bi@mail.com

## Schema file

    {
        "strict": "true",
        "name": "employee",
        "comments": [
            "--"
        ],
        "fields": [
            {
                "name": "id"
            },
            {
                "name": "firstname"
            },
            {
                "name": "lastname"
            },
            {
                "name": "zipcode"
            },
            {
                "name": "phone"
            },
            {
                "name": "email"
            }
        ],
        "tokenizer": {
            "type": "delimited",
            "delimiter": ",",
            "strict": true
        }
    }

## Output by flat2sql (log)

    Name:SQL-Trace, Connection:1, Time:8, Success:True
    Type:Statement, Batch:False, QuerySize:1, BatchSize:0
    Query:["CREATE TABLE IF NOT EXISTS employee ( id UUID not null default uuid_generate_v4(), foreign_id int not null, firstname varchar(255), lastname varchar(255), zipcode varchar(255), phone varchar(255), email varchar(255), primary key (id) )"]
    Params:[]
    Name:SQL-Trace, Connection:2, Time:21, Success:True
    Type:Prepared, Batch:True, QuerySize:1, BatchSize:5
    Query:["UPSERT INTO employee (id,foreign_id,firstname,lastname,zipcode,phone,email) VALUES (uuid_generate_v4(),?,?,?,?,?,?)"]
    Params:[(1,Deanna,Melanie,123 45,000-0000000,dm@mail.com),(2,Booker,Hudson,123 45,000-0000000,bh@mail.com),(3,Adriana,Alana,123 45,000-0000000,aa@mail.com),(4,Brande,Kelli,123 45,000-0000000,bk@mail.com),(5,Beatrice,India,123 45,000-0000000,bi@mail.com)]

    
# Fixed-length Flat File Example

## Source file

    <REM> A comment
    -- This is also a comment
    # This also
    UK21341EAH4121131.11customer1
    UK21341EAH4221232.11customer2
    UK21341EAH4321333.11customer3
    UK21341EAH4421434.11customer4
    UK21341EAH4521535.11customer5
    UK21341EAH4521535.11customer6
    UK21341EAH4521535.11customer7
    UK21341EAH4521535.11customer8
    UK21341EAH4521535.11customer9
    UK21341EAH4521535.11customer1
    UK21341EAH4121131.11customer1
    UK21341EAH4221232.11customer2

## Schema file

    {
        "strict": "true",
        "name": "orders",
        "comments": [
            "--",
            "<REM>",
            "<TERM>",
            "#"
        ],
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
