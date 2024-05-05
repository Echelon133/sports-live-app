package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.echelon133.competitionservice.competition.model.TeamStats;
import pl.echelon133.competitionservice.competition.repository.TeamStatsRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TeamStatsService {

    private final TeamStatsRepository teamStatsRepository;

    @Autowired
    public TeamStatsService(TeamStatsRepository teamStatsRepository) {
        this.teamStatsRepository = teamStatsRepository;
    }

    /**
     * Find {@link TeamStats} of a team with specified id that plays in a competition with specified id.
     *
     * @param teamId id of the team we are searching for
     * @param competitionId id of the competition in which the team plays
     * @return stats of a team in a particular competition
     * @throws ResourceNotFoundException thrown when stats for a team in a given competition do not exist
     */
    public TeamStats findTeamStats(UUID teamId, UUID competitionId) throws ResourceNotFoundException {
        return teamStatsRepository
                .findByTeamIdAndGroup_Competition_Id(teamId, competitionId)
                .orElseThrow(() -> new ResourceNotFoundException(TeamStats.class, teamId));
    }

    /**
     * Saves multiple {@link TeamStats} objects to database.
     * @param teamStats object to save
     * @return saved objects
     */
    public List<TeamStats> saveAll(Iterable<TeamStats> teamStats) {
        return teamStatsRepository.saveAll(teamStats);
    }
}
