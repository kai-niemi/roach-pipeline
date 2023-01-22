# Pipeline REST API Guide

This guide provides a description on how to use the Hypermedia/REST API. The API is based on 
the [HAL](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08) and 
[HAL+Forms](https://rwcbook.github.io/hal-forms/) media types. 

The REST principle is to follow links, read the response, discover options and decide
where to go next by again following links or submitting forms. The path is guided by the
server by using hypermedia affordances such as link rels and form elements. Clients of a 
REST API should not create URIs on their own, but instead follow the ones provided in 
responses. This is the part that enhances decoupling and independent client/server 
evolution.

See Appendix A for a general introduction to REST and Hypermedia APIs.

## Introduction

Basic tutorial for starting a `sql2csv` job using cURL. 

The first step is to navigate to the root of the API which will provide an index resource 
representation. The index is listing the core abilities of the API and the starting points
for a conversational workflow:

    curl -X GET http://localhost:8090

Next, look for the link rel `pipeline:sql2csv` URI and then GET the form template:

    curl -X GET http://localhost:8090/sql2csv/form > form.json

Lastly, populate the form template and POST it back to the `target` URI provided 
in the form:

    curl -d "@form.json" -H "Content-Type:application/json" -X POST http://localhost:8090/sql2csv

That is all there is to it.

# HTTP Methods

Supported HTTP verbs:

- GET: View a resource or a collection of resources (safe,idempotent)
- POST: Create a new resource (unsafe,non-idempotent)
- PUT: Modify a resource in its entirety (unsafe,idempotent)
- DELETE: Delete a given resource (unsafe,idempotent)

Method safety and idempotence:

- A safe method (GET,HEAD,OPTIONS) means triggering it once has the same effect as not triggering it all.
- An idempotent method (PUT, DELETE) means triggering it once or more than once has the same effect.
- Safe methods are implicitly idempotent.

# Link Relations

Link relations convey information about how clients can transition from one application state to another.
Each transition option has a corresponding link relation name. This API contains both domain specific 
link relation names and IANA registered link relation names for self reference and pagination of 
collection resources (first, last, next, prev).

## IANA Link Relations

For more information about common link relations, see [IANA Link Relations](http://www.iana.org/assignments/link-relations/link-relations.xhtml).

| Link Relation | Methods | Description                                         |
|---------------|---------|-----------------------------------------------------|
| self          | GET     | Retrieve the enclosed resource                      |
| first         | GET     | Retrieve the first page of items in a collection    |
| last          | GET     | Retrieve the last page of items in a collection     |
| next          | GET     | Retrieve the next page of items in a collection     |
| prev          | GET     | Retrieve the previous page of items in a collection |

## Domain Link Relations

Domain links with relations that are not registered with the [IANA registry](https://www.iana.org/assignments/link-relations/link-relations.xhtml) 
of link relation types are prefixed with the `pipeline` curie.

Resources for data pipeline jobs:

| Link Relation                         | Methods    | Description                                            | States    |
|---------------------------------------|------------|--------------------------------------------------------|-----------|
| [pipeline:cdc2sql](cdc2sql.md)     | GET        | Retrieve a form template for issuing a job             | Index     |
| [pipeline:csv2sql](csv2sql.md)     | GET        | Retrieve a form template for issuing a job             | Index     |
| [pipeline:flat2csv](flat2csv.md)   | GET        | Retrieve a form template for issuing a job             | Index     |
| [pipeline:kafka2sql](kafka2sql.md) | GET        | Retrieve a form template for issuing a job             | Index     |
| [pipeline:sql2csv](sql2csv.md)     | GET        | Retrieve a form template for issuing a job             | Index     |
| [pipeline:sql2sql](sql2sql.md)     | GET        | Retrieve a form template for issuing a job             | Index     |

Resources for control and observability:

| Link Relation              | Methods    | Description                                            | States    |
|----------------------------|------------|--------------------------------------------------------|-----------|
| pipeline:actuator       | GET        | Retrieve collection of Spring Boot actuators           | Index     |
| pipeline:jobs           | GET        | Retrieve a full collection of registered jobs          | Index     |
| pipeline:instance       | GET        | Retrieve details about a job instance                  | Jobs      |
| pipeline:instances      | GET        | Retrieve a full collection of registered job instances | Jobs      |
| pipeline:last-instance  | GET        | Retrieve last job instance                             | Jobs      |
| pipeline:execution      | GET        | Retrieve details about a job execution                 | Instance  |
| pipeline:executions     | GET        | Retrieve a full collection of job executions           | Instance  |
| pipeline:last-execution | GET        | Retrieve last job execution                            | Instance  |
| pipeline:step           | GET        | Retrieve details about a job step                      | Execution |
| pipeline:steps          | GET        | Retrieve a full collection of job steps                | Execution |
| pipeline:stop           | PUT,DELETE | Stop job execution                                     | Execution |

# Error Handling

All client or server problems will be served in the form of an `application/problem+json` 
[RFC 7807 Problem Detail](https://tools.ietf.org/html/rfc7807) response.

## Problem Properties

- type - URL describing type of problem that occurred
- status (number) - The HTTP status of the error
- detail - A human-readable explanation specific to this occurrence of the problem.

# Additional Resources

- [HAL](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08) media type
- [HAL+Forms](https://rwcbook.github.io/hal-forms/) media type
- [Spring HATEOAS](https://docs.spring.io/spring-hateoas/docs/current/reference/html)
- [Affordances](https://github.com/spring-projects/spring-hateoas-examples/tree/main/affordances)

# Appendix A: About REST/Hypermedia APIs

When using a Hypermedia API, a client can identify all information pieces and act accordingly by
understanding the media type specification, the domain semantics and by following standard HTTP idioms.

Depending on the current application state (view of the system), a client will be presented different data elements
and state transition options in form of links to follow. This allows a client (or user at design time)
to discover the API by making use of the contextual links and affordances returned in responses.

Rather than concatenating URL's and figuring out where to go next by out-of-band information,
a client follow link URIs based on the link relations provided in responses. These links are in turn
selectively provided by the server based on the resource state and business rules, enabling a
mechanism to guide the client through a series of workflow steps.

This is known as the _Hypermedia Constraint_ or _Hypermedia as The Engine of Application State (HATEOAS)_
with the benefits of reduced client/server coupling and less need for API versioning.

One key difference to RPC-styled APIs without hypermedia, is that you want to avoid constructing URIs
and having assumptions about the structure or semantics of the resource URIs. Instead, clients should 
bind to the semantic descriptors (affordances) such as link relations, data elements and form templates.
