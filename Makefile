MAVEN := ./mvnw

.PHONY: all
all: build

.PHONY: build
build: all
	mvn clean package

.PHONY: docker-up
docker-up: build
	docker-compose up -d

.PHONY: connectors
kafka-connectors:
	curl -XPUT -H 'Content-Type:application/json' -d @docker/connector/sf-sink.json http://localhost:8083/connectors/sf_sink/config


.PHONY: metadata-topic
create:
	docker-compose exec kafka kafka-topics --create --topic metadata-topic --bootstrap-server localhost:9092 schema.registry.url=schema-registry:8081 key.schema='{"type":"string"}' value.schema='{"type":"object", "properties":"topic":{"type":"string"}, "partition":{"type":"int"}, "offset":{"type":"int"},  "timestamp":{"type":"int"}, "headers": {"type":"list"}}'

.PHONY: compact-topic
compact-topic:
  docker-compose exec kafka kafka-topics --create --topic input-topic --bootstrap-server localhost:9092 --partitions 1 --config cleanup.policy=compact

.PHONY: delete-topic
delete-t:
  docker-compose exec kafka kafka-topics --delete --topic metadata-topic --bootstrap-server localhost:9092

.PHONY: delete-records
delete-r:
  docker-compose exec kafka kafka-delete-records  --bootstrap-server localhost:9092 --offset-json-file delete-records.json


.PHONY: produce-metadata
produce:
	docker-compose exec kafka kafka-console-producer  --topic metadata-topic --bootstrap-server localhost:9092 --property "parse.key=true" --property "key.separator=," --property schema.registry.url=schema-registry:8081 --property value.schema='{"type":"object", "properties":"topic":{"type":"string"}, "partition":{"type":"int"}, "offset":{"type":"int"},  "timestamp":{"type":"int"}, "headers": {"type":"list"}}'


.PHONY: compact-data
compact-data:
 	docker-compose exec kafka kafka-console-producer  --topic input-topic --bootstrap-server localhost:9092 --property "parse.key=true" --property "key.separator=,"  --property schema.registry.url=schema-registry:8081 --property key.schema='{"type":"string"}' --property value.schema='{"type":"object", "properties":"eClaims_Eligible_c":{"type":"string"}, "Id":{"type":"string"}, "_ObjectType":{"type":"string"} }'

.PHONY: consume-metadata
consume:
	docker-compose exec kafka kafka-console-consumer  --topic metadata-topic --bootstrap-server localhost:9092 --from-beginning --consumer.config /home/consumer.properties

