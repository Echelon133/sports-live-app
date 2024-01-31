package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.competition.dto.CompetitionDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Finds all competitions whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the competition
     * @param pageable information about the wanted page
     * @return a page of competitions which match the filter
     */
    public Page<CompetitionDto> findCompetitionsByName(String phrase, Pageable pageable) {
        return competitionRepository.findAllByNameContaining(phrase, pageable);
    }
}
