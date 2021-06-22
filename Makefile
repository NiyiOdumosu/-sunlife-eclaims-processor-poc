MAVEN := ./mvnw

.PHONY: all
all: build

.PHONY: build
build: all
	mvn clean package

.PHONY: docker-up
docker-up: build
	docker-compose up -d

.PHONY: docker-down
docker-down: build
	docker-compose up -d

.PHONY: connectors
connectors:
	curl -XPUT -H 'Content-Type:application/json' -d @docker/connector/sf-sink.json http://localhost:8083/connectors/sf_sink/config | jq .


.PHONY: metadata-topic
metadata-topic:
	kafka-topics --create --topic metadata-topic --bootstrap-server localhost:9092 schema.registry.url=schema-registry:8081 key.schema='{"type":"string"}' value.schema='{"type":"object", "properties":"topic":{"type":"string"}, "partition":{"type":"int"}, "offset":{"type":"int"},  "timestamp":{"type":"int"}, "headers": {"type":"list"}}'

.PHONY: compact-topic
compact-topic:
    kafka-topics --create --topic input-topic --bootstrap-server localhost:9092 --partitions 1 --config cleanup.policy=compact


.PHONY: produce-compacted-data
produce-compacted-data:
 	kafka-console-producer  --topic input-topic --bootstrap-server localhost:9092 --property "parse.key=true" --property "key.separator=,"  --property schema.registry.url=schema-registry:8081 --property key.schema='{"type":"string"}' --property value.schema='{"type":"object", "properties":"eClaims_Eligible_c":{"type":"string"}, "Id":{"type":"string"}, "_ObjectType":{"type":"string"} }'


.PHONY: produce-metadata
produce-metadata:
	kafka-console-producer  --topic metadata-topic --bootstrap-server localhost:9092 --property "parse.key=true" --property "key.separator=," --property schema.registry.url=schema-registry:8081 --property value.schema='{"type":"object", "properties":"topic":{"type":"string"}, "partition":{"type":"int"}, "offset":{"type":"int"},  "timestamp":{"type":"int"}, "headers": {"type":"list"}}'


.PHONY: consume-metadata
consume-metadata:
	kafka-console-consumer  --topic metadata-topic --bootstrap-server localhost:9092 --from-beginning


.PHONY: consume-compacted-topic
consume-compacted-topic:
	kafka-console-consumer  --topic input-topic --bootstrap-server localhost:9092 --from-beginning


.PHONY: delete-topic
delete-topic:
   kafka-topics --delete --topic metadata-topic --bootstrap-server localhost:9092


.PHONY: delete-records
delete-records:
  kafka-delete-records  --bootstrap-server localhost:9092 --offset-json-file delete-records.json

