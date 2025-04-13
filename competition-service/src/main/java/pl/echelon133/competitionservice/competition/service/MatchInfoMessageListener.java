package pl.echelon133.competitionservice.competition.service;

import jakarta.transaction.Transactional;
import ml.echelon133.common.event.dto.MatchInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.MessageListener;
import pl.echelon133.competitionservice.competition.model.UnassignedMatch;
import pl.echelon133.competitionservice.competition.repository.CompetitionMatchRepository;
import pl.echelon133.competitionservice.competition.repository.UnassignedMatchRepository;

import java.util.UUID;

@Transactional
public class MatchInfoMessageListener implements MessageListener<UUID, MatchInfo> {

    private final Logger logger = LoggerFactory.getLogger(MatchInfoMessageListener.class);
    private final UnassignedMatchRepository unassignedMatchRepository;
    private final CompetitionMatchRepository competitionMatchRepository;

    public MatchInfoMessageListener(
            UnassignedMatchRepository unassignedMatchRepository,
            CompetitionMatchRepository competitionMatchRepository
    ) {
        this.unassignedMatchRepository = unassignedMatchRepository;
        this.competitionMatchRepository = competitionMatchRepository;
    }

    @Override
    public void onMessage(ConsumerRecord<UUID, MatchInfo> record) {
        var matchId = record.value().matchId();
        var competitionId = record.value().competitionId();
        logger.info("Received key {} with matchId {} and competitionId {}",
                record.key(), matchId, competitionId
        );
        switch (record.value()) {
            case MatchInfo.CreationEvent creationEvent -> {
                unassignedMatchRepository.save(new UnassignedMatch(matchId, competitionId));
                logger.info("Created unassigned match with matchId {} and competitionId {}", matchId, competitionId);
            }
            case MatchInfo.FinishEvent finishEvent -> {
                // if a match is finished, it should be marked as such in two places:
                //      * UnassignedMatch entity's table
                //      * CompetitionMatch entity's table

                // try to mark the match as finished in the UnassignedMatch table
                var unassignedMatch = unassignedMatchRepository
                        .findById(new UnassignedMatch.UnassignedMatchId(matchId, competitionId));
                if (unassignedMatch.isPresent()) {
                    var uMatch = unassignedMatch.get();
                    uMatch.setFinished(true);
                    unassignedMatchRepository.save(uMatch);
                    logger.info(
                            "Unassigned match with matchId {} and competitionId {} marked as finished",
                            matchId, competitionId
                    );
                }

                // try to mark the match as finished in the CompetitionMatch table
                var competitionMatch = competitionMatchRepository.findByMatchId(matchId);
                if (competitionMatch.isPresent()) {
                    var cMatch = competitionMatch.get();
                    cMatch.setFinished(true);
                    competitionMatchRepository.save(cMatch);
                    logger.info(
                            "Competition match with matchId {} and competitionId {} marked as finished",
                            matchId, competitionId
                    );
                }
            }
        }
    }
}
