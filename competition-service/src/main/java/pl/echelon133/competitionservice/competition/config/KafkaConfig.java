package pl.echelon133.competitionservice.competition.config;

import ml.echelon133.common.event.KafkaTopicNames;
import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.common.event.dto.MatchInfo;
import ml.echelon133.common.event.dto.kafka.MatchEventDetailsDeserializer;
import ml.echelon133.common.event.dto.kafka.MatchInfoDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import pl.echelon133.competitionservice.competition.repository.CompetitionMatchRepository;
import pl.echelon133.competitionservice.competition.repository.UnassignedMatchRepository;
import pl.echelon133.competitionservice.competition.service.MatchEventDetailsMessageListener;
import pl.echelon133.competitionservice.competition.service.MatchInfoMessageListener;
import pl.echelon133.competitionservice.competition.service.PlayerStatsService;
import pl.echelon133.competitionservice.competition.service.TeamStatsService;

import java.util.Map;
import java.util.UUID;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final PlayerStatsService playerStatsService; // required by the MatchEventDetailsMessageListener
    private final TeamStatsService teamStatsService;     // required by the MatchEventDetailsMessageListener
    private final UnassignedMatchRepository unassignedMatchRepository; // required by the MatchInfoMessageListener
    private final CompetitionMatchRepository competitionMatchRepository; // required by the MatchInfoMessageListener

    @Autowired
    public KafkaConfig(
            PlayerStatsService playerStatsService,
            TeamStatsService teamStatsService,
            UnassignedMatchRepository unassignedMatchRepository,
            CompetitionMatchRepository competitionMatchRepository
    ) {
        this.playerStatsService = playerStatsService;
        this.teamStatsService = teamStatsService;
        this.unassignedMatchRepository = unassignedMatchRepository;
        this.competitionMatchRepository = competitionMatchRepository;
    }

    @Bean
    KafkaMessageListenerContainer<UUID, MatchEventDetails> matchEventDetailsListenerContainer() {
        ContainerProperties containerProps = new ContainerProperties(KafkaTopicNames.MATCH_EVENTS);
        containerProps.setMessageListener(
                new MatchEventDetailsMessageListener(playerStatsService, teamStatsService)
        );

        ConsumerFactory<UUID, MatchEventDetails> consumerFactory = matchEventDetailsConsumerFactory();

        return new KafkaMessageListenerContainer<>(
                consumerFactory, containerProps
        );
    }

    private ConsumerFactory<UUID, MatchEventDetails> matchEventDetailsConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "competition-service-group"
        );
        return new DefaultKafkaConsumerFactory<>(
                props,
                new UUIDDeserializer(),
                new MatchEventDetailsDeserializer()
        );
    }

    @Bean
    KafkaMessageListenerContainer<UUID, MatchInfo> matchInfoListenerContainer() {
        ContainerProperties containerProps = new ContainerProperties(KafkaTopicNames.MATCH_INFO);
        containerProps.setMessageListener(new MatchInfoMessageListener(unassignedMatchRepository, competitionMatchRepository));

        ConsumerFactory<UUID, MatchInfo> consumerFactory = matchInfoConsumerFactory();

        return new KafkaMessageListenerContainer<>(
                consumerFactory, containerProps
        );
    }

    private ConsumerFactory<UUID, MatchInfo> matchInfoConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "competition-service-group"
        );
        return new DefaultKafkaConsumerFactory<>(
                props,
                new UUIDDeserializer(),
                new MatchInfoDeserializer()
        );
    }
}
