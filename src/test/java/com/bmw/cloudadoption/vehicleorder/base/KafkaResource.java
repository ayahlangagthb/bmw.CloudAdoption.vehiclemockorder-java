package com.bmw.cloudadoption.vehicleorder.base;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.*;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;

public class KafkaResource implements QuarkusTestResourceLifecycleManager {

    private static KafkaContainer KAFKA;
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka");
    private static final String DEFAULT_TAG = "5.5.0";

    /**
     * Start the Kafka test container and set the bootstrap server as system
     * property, so that our Quarkus application can pick that config property
     * up while starting and therefore connect to that container as its Kafka
     * broker.
     */
    @Override
    public Map<String, String> start() {
        String bootstrapServer;

        KAFKA = new KafkaContainer(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));

        /*
             * added this for failing tests because of "exactly-once" semantic changes..
             * below solution has been taken from the link:
             * https://github.com/testcontainers/testcontainers-java/issues/1816
         */
        KAFKA.addEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
        KAFKA.addEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");
        KAFKA.start();
        bootstrapServer = KAFKA.getBootstrapServers();

        System.setProperty("quarkus.kafka-streams.bootstrap-servers", bootstrapServer); // Used for Kafka-Streams
        System.setProperty("kafka.bootstrap.servers", bootstrapServer); // Used for reactive messaging Kafka
        System.setProperty("quarkus.kafka-streams.application-id", "vehiclemockorder-" + UUID.randomUUID().toString()); // So that our local state store on disk isn't reused between tests

        short replicationFactor = 1; // Replication Factor MUST be 1, our testcontainer is only a single instance!
        // Create topics
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        Collection<NewTopic> newTopics = Stream.of("bmw.cloudadoption.VehicleMockOrder.v1")
                .map(topic -> new NewTopic(topic, 10, replicationFactor)).collect(toList());
        try ( AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(newTopics);
        }
        return Collections.emptyMap();
    }

    /**
     * Remove all properties set before starting our tests.
     */
    @Override
    public void stop() {
        System.clearProperty("quarkus.kafka-streams.bootstrap-servers");
        System.clearProperty("kafka.bootstrap.servers");
        System.clearProperty("quarkus.kafka-streams.application-id");
        if (KAFKA != null) {
            KAFKA.close();
        }

    }
}
