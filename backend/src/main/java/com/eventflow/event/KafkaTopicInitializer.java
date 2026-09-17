package com.eventflow.event;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class KafkaTopicInitializer {

    private static final Logger LOG = Logger.getLogger(KafkaTopicInitializer.class);
    private static final String TOPIC = "order-created";

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    void onStart(@Observes StartupEvent event) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);

        Map<String, String> topicConfig = new HashMap<>();
        topicConfig.put(TopicConfig.RETENTION_MS_CONFIG, "604800000");

        NewTopic newTopic = new NewTopic(TOPIC, 1, (short) 1).configs(topicConfig);

        try (AdminClient adminClient = AdminClient.create(props)) {
            Set<String> existing = adminClient.listTopics().names().get();
            if (!existing.contains(TOPIC)) {
                adminClient.createTopics(Set.of(newTopic)).all().get();
                LOG.infof("Created Kafka topic %s with 1 partition, RF=1, retention=604800000ms", TOPIC);
            } else {
                LOG.infof("Kafka topic %s already exists", TOPIC);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while initializing Kafka topic", ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                LOG.infof("Kafka topic %s already exists", TOPIC);
            } else {
                LOG.warnf(ex, "Could not initialize Kafka topic %s; publisher/consumer may retry later", TOPIC);
            }
        }
    }
}
