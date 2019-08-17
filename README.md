[![Build Status](https://travis-ci.org/juliuskrah/antaeus.svg?branch=master)](https://travis-ci.org/juliuskrah/antaeus)

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

### Building

```
./gradlew build
```

### Running

Docker Compose is required to run.


*Running through docker*

Install docker compose for your platform

```bash
> docker-compose up -d --scale antaeus=3
```

The above starts three instances of the antaeus app

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the "rest api" models used throughout the application.
|
├── pleo-antaeus-rest
|       Entry point for REST API. This is where the routes are defined.
|
├── pleo-antaeus-schedule
|       Module containing the APIs for scheduling and running payment of invoices.
└──
```

## The Solution
The problem domain here is with scheduling payment of invoices, so the focus will be on scheduling.
To achieve this, I will use the Quartz Scheduling library for the JVM.

A new module has been added `pleo-antaeus-schedule` to abstract scheduling related activities. This module
contains two jobs `PendingInvoiceJob` and `BillingJob`.

`PendingInvoiceJob` is scheduled to run on the 1st of each month. Once run, it fetches all pending invoices
and schedules each invoice as `BillingJob` to run immediately. A thread pool of 5 is initialized to cater 
for the numerous jobs.
This is configurable in a `properties` file (quartz.properties).

The `PaymentProvider#charge(Invoice)` throws three possible exceptions
- `CustomerNotFoundException`: A business rule can be used to determine what to do when customer is not found
- `CurrencyMismatchException`: A business rule can be used to determine what to do when there's a currency mismatch
- `NetworkException`: In case of a network exception, there's a retry loop. The number of retries is 
configurable with an environment variable (`MAX_RETRIES`)

### Runtime Configuration
The problem domain is with scheduling. We need a way to add new schedules e.g. try to charge invoices three times a month. We can also update the schedule.

Three endpoints are added in the `pleo-antaeus-rest` module:

- Fetch all jobs

```json
GET: http:$HOST:$PORT/rest/v1/jobs

[
    {
        "group": "antaeus",
        "name": "invoices",
        "triggers": [
            {
                "group": "antaeus",
                "name": "invoices",
                "cron": ""
            }
        ]
    }
]
```

- Create a job

```json
POST: http:$HOST:$PORT/rest/v1/jobs

 {
    "group": "antaeus",
    "name": "invoices",
    "triggers": [
        {
            "group": "antaeus",
            "name": "invoices1",
            "cron": "0 0 12 1 1/1 ? *"
        }
    ]
}
```

- Update a job

```json
PUT: http:$HOST:$PORT/rest/v1/jobs

 {
    "group": "antaeus",
    "name": "invoices",
    "triggers": [
        {
            "group": "antaeus",
            "name": "invoices1",
            "cron": "0 0 12 1 1/1 ? *"
        }
    ]
}
```

### Testing
Test cases have been written for all new service methods and REST endpoints. Continuous integration
with travis has been added to ensure the tests also pass on another machine.

### Scaling
When the application is scaled horizontally, there is the possibility, scheduled tasks will run 
concurrently on all instances causing duplicate charges.

Quartz provides cluster support through its JDBC jobstore ensuring only one instance executes the 
scheduled task. Postgres database has been added to provide a shared database for the scheduled
quartz instances.

### Limitations
- [x] Security: The implementation here lacks security. One solution is to protect it with OpenID Connect
- [x] When loading potentially large datasets of invoices, this could lead to out of memory errors. One solution is to fetch in batches
- [x] Retries: The retry happens immediately. A production ready system will have some sort of an 
exponential backoff mechanism to allow the external system to recover

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine
* [Quartz](http://www.quartz-scheduler.org/) - Scheduling library

Happy hacking 😁!
