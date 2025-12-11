package com.startupheroes.app.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static org.springframework.util.StringUtils.hasText;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private final KafkaProperties springKafkaProps;

    @Bean
    public NewTopic packageTopic() {
        String topicName = springKafkaProps.getTemplate().getDefaultTopic();

        if (!hasText(topicName)) {
            throw new IllegalStateException("Kafka default topic name is not configured");
        }
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
