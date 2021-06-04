# Sunlife Eclaims Processor POC


This repository provides a POC demo of the sunlife eclaims reprocessing flow for SalesForce connector.

## How to run

First build the project. You can use the `make` commands to build and run the project.

```make build```

This does a mvn clean package.

``` make docker-up```

This starts all the containers in the docker-compose file.

```make connector```

This deploys the connector configs in the `docker/connector` directory to the local connect instance running.

The connector config is meant to be modified to use your own SalesForce credentials.

```
"salesforce.username": "user",
"salesforce.password": "password",
"salesforce.password.token": "*******",
"salesforce.consumer.key": "*****",
"salesforce.consumer.secret": "****************",
"salesforce.instance": "https://test.salesforce.com"
``` 


