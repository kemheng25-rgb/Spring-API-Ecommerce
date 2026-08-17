package com.example.ecommerce.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ORDER_EVENTS_TOPIC = "order.events";

    /** Spring Boot's auto-configured KafkaAdmin creates this topic on startup if it's missing. */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
}
