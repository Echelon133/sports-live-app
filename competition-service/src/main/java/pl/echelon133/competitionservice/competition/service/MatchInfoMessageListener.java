package pl.echelon133.competitionservice.competition.service;

import jakarta.transaction.Transactional;
import ml.echelon133.common.event.dto.MatchInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.MessageListener;
import pl.echelon133.competitionservice.competition.model.UnassignedMatch;
import pl.echelon133.competitionservice.competition.repository.UnassignedMatchRepository;

import java.util.UUID;

@Transactional
public class MatchInfoMessageListener implements MessageListener<UUID, MatchInfo> {

    private final Logger logger = LoggerFactory.getLogger(MatchInfoMessageListener.class);
    private final UnassignedMatchRepository unassignedMatchRepository;

    public MatchInfoMessageListener(UnassignedMatchRepository unassignedMatchRepository) {
        this.unassignedMatchRepository = unassignedMatchRepository;
    }

    @Override
    public void onMessage(ConsumerRecord<UUID, MatchInfo> record) {
        var matchId = record.value().matchId();
        var competitionId = record.value().competitionId();
        logger.info("Received key {} with matchId {} and competitionId {}",
                record.key(), matchId, competitionId
        );
        var unassignedMatch = unassignedMatchRepository.save(new UnassignedMatch(matchId, competitionId));
        logger.info(
                "Saved unassigned match with matchId {} and competitionId {} as {}",
                matchId, competitionId, unassignedMatch.getId()
        );
    }
}
