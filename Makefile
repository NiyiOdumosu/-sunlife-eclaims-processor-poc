MAVEN := ./mvnw

.PHONY: all
all: build

.PHONY: build
build: all
	${MAVEN} clean package

.PHONY: docker-up
docker-up: build
	docker-compose up -d

.PHONY: connectors
kafka-connectors:
	curl -XPUT -H 'Content-Type:application/json' -d @docker/connector/sf-sink.json http://localhost:8083/connectors/sf_sink/config


.PHONY: metdata-topic
produce:
	docker-compose exec kafka kafka-topics --create --topic metadata-topic --bootstrap-server kafka:9092 --property schema.registry.url=schema-registry:8081 --property value.schema='{"type":"object", "properties":{"topic":{"type":"string"}, {"partition":{"type":"int"}, {"offset":{"type":"int"}},  {"timestamp":{"type":"int"}, "headers": {"type":"list"}}'

.PHONY: compact-topic
compact:
  docker-compose exec kafka kafka-topics --create --topic compact-topic --bootstrap-server kafka:9092 --partitions 1 --config cleanup.policy=compact


.PHONY: produce-metdata
produce:
	docker-compose exec kafka kafka-console-producer  --topic metadata-topic --bootstrap-server kafka:9092 --property schema.registry.url=schema-registry:8081 --property value.schema='{"type":"object", "properties":{"topic":{"type":"string"}, {"partition":{"type":"int"}, {"offset":{"type":"int"}},  {"timestamp":{"type":"int"}, "headers": {"type":"list"}}'

.PHONY: consume-metadata
consume:
	docker-compose exec kafka kafka-console-consumer  --topic metadata-topic --bootstrap-server kafka:9092 --from-beginning

