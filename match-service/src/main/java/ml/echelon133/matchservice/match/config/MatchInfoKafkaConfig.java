package ml.echelon133.matchservice.match.config;

import ml.echelon133.common.event.KafkaTopicNames;
import ml.echelon133.common.event.dto.MatchInfo;
import ml.echelon133.common.event.dto.kafka.MatchInfoSerializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;
import java.util.UUID;

@Configuration
public class MatchInfoKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public NewTopic matchInfoTopic() {
        return TopicBuilder
                .name(KafkaTopicNames.MATCH_INFO)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public KafkaProducer<UUID, MatchInfo> matchInfoKafkaProducer() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
        );
        return new KafkaProducer<>(
                props,
                new UUIDSerializer(),
                new MatchInfoSerializer()
        );
    }
}
