package sunlife.eclaims.poc;

import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
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
import sunlife.eclaims.poc.model.Header;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

public class EclaimsErrorProcessor {



    Consumer<String, EclaimErrorObject> consumer;
    Logger logger = LoggerFactory.getLogger(EclaimsErrorProcessor.class);

    private static final String childTopic = "metadata-topic";
    private static final String parentTopic = "compacted-topic";
    private static final String conf = "/Users/nodumosu/.confluent/java.config";
    private static final String yamlConf = "/Users/nodumosu/workspaces/sunlife-eclaims-processor-poc/src/main/resources/config.yaml";
    private String offset = "";
    private String partition = "";
    private String topic = "";
    private String timestamp = "";

    public void consumeRecords(String configFile) {
        Properties props;

        try {
            props = loadConfig(configFile);
            consumer = new KafkaConsumer<String, EclaimErrorObject>(props);
            consumer.subscribe(Arrays.asList(childTopic));
            ConsumerRecords<String, EclaimErrorObject> records = consumer.poll(1000);


            for (ConsumerRecord<String, EclaimErrorObject> record : records) {
                String key = record.key();
                // parse metadata from record.value()
                EclaimErrorObject metadata = record.value();

                LinkedList<Header> headers = metadata.getHeaders();
                for (Header header : headers){
                    if (header.getKey().equals("input_record_topic")){
                        topic = header.getStringValue();
                    }
                    else if (header.getKey().equals("input_record_offset")){
                        offset = header.getStringValue();
                    }
                    else if (header.getKey().equals("input_record_timestamp")){
                        timestamp = header.getStringValue();
                    }
                    else if (header.getKey().equals("input_record_partition")){
                        partition = header.getStringValue();
                    }
                    // use metadata to consume from the parent topic
                    consumeInputTopic(topic, partition, offset, props);
                }
                logger.info("Consumed record with key" + key + "and value" + headers);
            }
        } catch (IOException | URISyntaxException e) {
            logger.error("Consumer failed to consume due to the following: %s", e.getMessage());
//        logger.log("error", "Consumer failed to consume due to the following:" + e.getMessage())
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }


    public void consumeInputTopic(String topic, String partition, String offset, Properties props){
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        Consumer inputConsumer = new KafkaConsumer<String, String>(props);

        inputConsumer.subscribe(Arrays.asList(topic));
        inputConsumer.seek(new TopicPartition(topic, Integer.parseInt(partition)), Long.parseLong(offset));
        ConsumerRecords<String, String> inputRecords = inputConsumer.poll(1000);
        if(!inputRecords.isEmpty()){
            for (ConsumerRecord<String, String> record: inputRecords){
                logger.info("Here is the consumer record %s", record.toString());
                produceRecords(record, props, topic);
            }
        }
        else{
            logger.info("The record at offset " + offset + " has already been compacted");
        }
    }

    public void produceRecords(ConsumerRecord<String, String> consumeRecord, Properties config, String topic) {
        Producer<String, String> producer = new KafkaProducer<String, String>(config);
        try {
            producer.send(new
                    ProducerRecord<String, String>(topic, consumeRecord.key(), consumeRecord.value()));
        } catch (Exception e) {
            logger.error("Producer failed to consume due to the following: %s", e.getMessage());
        }
        producer.flush();
        producer.close();
    }


    public static Properties loadConfig(final String configFile) throws IOException, URISyntaxException {
        if (!Files.exists(Path.of(new URI("file:///" +configFile)))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }

        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(new File(yamlConf));
        Map<String, Object> config = yaml.load(inputStream);
        System.out.println(config);
        // consumer configs
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonDeserializer.class);
        cfg.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, EclaimErrorObject.class);
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "metadata-consumer-1");
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // producer configs
        cfg.put(ProducerConfig.ACKS_CONFIG, "1");
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSerializer.class);

        return cfg;
    }

    public static void main(final String[] args) throws IOException {
       EclaimsErrorProcessor processor = new EclaimsErrorProcessor();
        processor.consumeRecords(conf);
    }

}
