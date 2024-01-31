package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.competition.dto.CompetitionDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;

import java.util.UUID;

@Service
public class CompetitionService {

    private final CompetitionRepository competitionRepository;

    @Autowired
    public CompetitionService(CompetitionRepository competitionRepository) {
        this.competitionRepository = competitionRepository;
    }

    /**
     * Returns the information about the competition with specified id.
     *
     * @param competitionId id of the competition
     * @return a dto representing the competition
     * @throws ResourceNotFoundException thrown when the competition does not exist in the database
     */
    public CompetitionDto findById(UUID competitionId) throws ResourceNotFoundException {
        return competitionRepository
                .findCompetitionById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException(Competition.class, competitionId));
    }

    /**
     * Marks a competition with the specified id as deleted.
     *
     * @param id id of the competition to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markCompetitionAsDeleted(UUID id)  {
        return competitionRepository.markCompetitionAsDeleted(id);
    }
}
