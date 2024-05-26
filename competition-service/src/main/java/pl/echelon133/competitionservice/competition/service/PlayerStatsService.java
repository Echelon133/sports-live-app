package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.PlayerStats;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;
import pl.echelon133.competitionservice.competition.repository.PlayerStatsRepository;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
@Transactional
public class PlayerStatsService {

    private final Logger logger = LoggerFactory.getLogger(PlayerStatsService.class);
    private final PlayerStatsRepository playerStatsRepository;
    private final CompetitionRepository competitionRepository;

    @Autowired
    public PlayerStatsService(PlayerStatsRepository playerStatsRepository, CompetitionRepository competitionRepository) {
        this.playerStatsRepository = playerStatsRepository;
        this.competitionRepository = competitionRepository;
    }

    /**
     * Find {@link PlayerStats} of a player with specified id who plays in a competition with specified id.
     * If such stats are not found, create an empty stats object using provided <u>playerId</u>, <u>defaultTeamId</u>, and
     * <u>defaultName</u>. Fields <u>defaultTeamId</u> and <u>defaultName</u> are ignored if stats of the player are
     * found in the database.
     *
     * @param playerId id of the player we are searching for
     * @param competitionId id of the competition in which the player plays
     * @param defaultTeamId id of the team for which the player plays (in case the player is not found in the database)
     * @param defaultName name of the player (in case the player is not found in the database)
     * @return stats of a player found in the database or a new stats object with all stats set to 0
     */
    public PlayerStats findPlayerStatsOrDefault(
            UUID playerId,
            UUID competitionId,
            UUID defaultTeamId,
            String defaultName
    ) throws ResourceNotFoundException {
        var stats = playerStatsRepository.findByPlayerIdAndCompetition_Id(playerId, competitionId);

        if (stats.isPresent()) {
            logger.debug("Found stats of player {} in competition {}", playerId, competitionId);
            return stats.get();
        } else {
            logger.debug(
                    "Creating new stats of player {} in competition {} with name {} and teamId {}",
                    playerId, competitionId, defaultName, defaultTeamId
            );

            var competition = competitionRepository.findById(competitionId).orElseThrow(() -> {
                        logger.warn("Could not find competition {} to create stats for player {}", competitionId, playerId);
                        return new ResourceNotFoundException(Competition.class, competitionId);
            });

            var newPlayerStats = new PlayerStats(playerId, defaultTeamId, defaultName);
            newPlayerStats.setCompetition(competition);
            return newPlayerStats;
        }
    }

    /**
     * Saves {@link PlayerStats} object to database.
     * @param playerStats object to save
     * @return saved object
     */
    public PlayerStats save(PlayerStats playerStats) {
        return playerStatsRepository.save(playerStats);
    }
}
