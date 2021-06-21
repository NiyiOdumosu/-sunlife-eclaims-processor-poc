# Sunlife Eclaims Processor POC


This repository provides a POC demo of the sunlife eclaims reprocessing flow for SalesForce connector.

## How to run

First build the project. You can use the `make` commands to build and run the project.

```make build```

This does a mvn clean package.

``` make docker-up```

This command starts up all the docker containers. 

``` make metadata-topic```
This command creates an error topic where you can produce the SF error records to.

``` make compact-topic```
This command creates a compacted topic that has the eclaim records.

``` make produce-compacted-data```
This command produces data to a compacted topic that has the eclaim records. You can use the records from the `dummy-data` file.

``` make produce-metadata```
This command produces to a compacted topic that has the eclaim records. You can use the records from the `dummy-data` file.

``` make consume-compacted-topic```
This command consumes from the compacted topic that has the eclaim records. You can use the records from the `dummy-data` file.

## View Topics and Messages in the Control-Center

localhost:9021

### Topic Configuration

After you create the compacted topic, you need to modify the configuration settings of the topic to enforce the compaction. The compacted topic payload is below:

```
{ 
    "eclaims_Eligible_c": "No", 
    "id": "000294833000539836", 
    "_ObjectType": "AccountContactRelation" 
}
```

The below diagram shows how compaction works.


The properties below should be configured to enforce compaction:

[segment.bytes](https://docs.confluent.io/platform/current/installation/configuration/topic-configs.html#topicconfigs_segment.bytes) - the max size for specified for each segment in a partition in the topic.

[min.cleanable.dirty.ratio](https://docs.confluent.io/platform/current/installation/configuration/topic-configs.html#topicconfigs_min.cleanable.dirty.ratio) The frequencey in which a log compactor will attempt to clean a log based on the percentage of multiple values per a key.

[min.compaction.lag.ms](https://docs.confluent.io/platform/current/installation/configuration/topic-configs.html#topicconfigs_min.compaction.lag.ms) The minimum time a message will remain uncompacted in the log.

[max.compaction.lag.ms](https://docs.confluent.io/platform/current/installation/configuration/topic-configs.html#topicconfigs_max.compaction.lag.ms) The maximum time a message will remain uncompacted in the log.

The recommended settings for these configurations to start as follows:

segement.bytes = 1000
min.cleanable.dirty.ratio=0.1
min.compaction.lag.ms=0
max.compaction.lag=0

Please note that these values are subject to change based on the use case and desired performance of the clients interacting with the compacted topic.