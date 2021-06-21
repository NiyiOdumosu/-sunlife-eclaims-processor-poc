package sunlife.eclaims.poc;

import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
import io.confluent.kafka.serializers.KafkaJsonSerializerConfig;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import sunlife.eclaims.poc.model.EclaimErrorObject;
import sunlife.eclaims.poc.model.EclaimObject;
import sunlife.eclaims.poc.model.Header;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public class EclaimsErrorProcessor {

    Consumer<String, EclaimErrorObject> consumer;

    Logger logger = LoggerFactory.getLogger(EclaimsErrorProcessor.class);


    public void consumeRecords(String configFile) {
        /**
         * This method consumes records that failed to get produced to SalesForce through the SObject SinkConnector
         */
        Properties props;
        Map<String, String> config;

        try {

            props = loadConfig(configFile);
            config = getYamlConfig();
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList(config.get("errorTopic")));
            ConsumerRecords<String, EclaimErrorObject> errorRecords = consumer.poll(Duration.ofSeconds(5));
            if (!errorRecords.isEmpty()) {
                for (ConsumerRecord<String, EclaimErrorObject> record : errorRecords) {
                    String key = record.key();
                    // parse metadata from record.value()
                    EclaimErrorObject metadata = record.value();
                    String offset = "";
                    String partition = "";
                    String topic = "";
                    LinkedList<Header> headers = metadata.getHeaders();
                    for (Header header : headers) {
                        if ("input_record_topic".equals(header.getKey())) {
                            topic = header.getStringValue();
                        } else if ("input_record_offset".equals(header.getKey())) {
                            offset = header.getStringValue();
                        } else if ("input_record_partition".equals(header.getKey())) {
                            partition = header.getStringValue();
                        }
                    }
                    // use metadata to consume from the parent topic
                    logger.info("Consumed record with key" + key + "and value" + headers);
                    consumeInputTopic(topic, partition, offset, props);

                }
            }

        } catch (IOException | URISyntaxException  e) {
            logger.error("Consumer failed to consume due to the following: %s", e.getMessage());
            e.printStackTrace();
        }
    }


    public void consumeInputTopic(String topic, String partition, String offset, Properties props) throws FileNotFoundException {
        Map<String, String> config = getYamlConfig();
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.get("input-consumer-group"));
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonDeserializer.class);
        props.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, EclaimObject.class);
        Consumer inputConsumer = new KafkaConsumer<>(props);

        TopicPartition topicPartition = new TopicPartition(topic, Integer.parseInt(partition));
        List<TopicPartition> tps = Arrays.asList(topicPartition);
        inputConsumer.assign(tps);
        inputConsumer.seek(topicPartition, Long.parseLong(offset));
        ConsumerRecords<String, EclaimObject> inputRecords = inputConsumer.poll(Duration.ofSeconds(5));
        if(!inputRecords.isEmpty()){
            for (ConsumerRecord<String, EclaimObject> record: inputRecords){
                logger.info("Here is the consumer record %s", record.toString());
                // only produce the record if the seeked offset is returned and the message is not null (not compacted)
                if (record.offset() == Long.parseLong(offset) && !record.value().toString().isEmpty()){
                    produceRecords(record, props, topic);
                }
            }
        }
        else{
            logger.info("The record at offset " + offset + " has already been compacted");
        }

    }

    public void produceRecords(ConsumerRecord<String, EclaimObject> consumeRecord, Properties config, String topic) {
        Producer producer = new KafkaProducer<>(config);
        try {
            producer.send(new ProducerRecord<String, EclaimObject>(topic, consumeRecord.key(), consumeRecord.value()));
            logger.info("Produced new record to the following topic: %s", topic);
        } catch (Exception e) {
            logger.error("Producer failed to consume due to the following: %s", e.getMessage());
        }
        producer.flush();

    }


    public static Properties loadConfig(final String configFile) throws IOException, URISyntaxException {
        if (!Files.exists(Path.of(new URI("file:///" +configFile)))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }

        Map<String, String> config = getYamlConfig();

        // consumer configs
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonDeserializer.class);
        cfg.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, EclaimErrorObject.class);
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, config.get("error-consumer-group"));
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // producer configs
        cfg.put(ProducerConfig.ACKS_CONFIG, "1");
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSerializer.class);

        return cfg;
    }

    private static Map<String, String> getYamlConfig() throws FileNotFoundException {
        String yamlConf = new File("src/main/resources/config.yaml")
                .getAbsolutePath();
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(new File(yamlConf));
        Map<String, String> config = yaml.load(inputStream);
        return config;
    }

    public static void main(final String[] args) throws IOException {
        String configFile = new File("src/main/resources/bootstrap.config")
                .getAbsolutePath();
        EclaimsErrorProcessor processor = new EclaimsErrorProcessor();
        processor.consumeRecords(configFile);
    }

}
