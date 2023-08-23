package ml.echelon133.matchservice.coach.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.coach.dto.CoachDto;
import ml.echelon133.matchservice.coach.model.Coach;
import ml.echelon133.matchservice.coach.model.UpsertCoachDto;
import ml.echelon133.matchservice.coach.repository.CoachRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
@Transactional
public class CoachService {

    private final CoachRepository coachRepository;

    @Autowired
    public CoachService(CoachRepository coachRepository) {
        this.coachRepository = coachRepository;
    }

    private static CoachDto entityToDto(Coach coach) {
        return CoachDto.from(coach.getId(), coach.getName());
    }

    /**
     * Returns the information about the coach with specified id.
     *
     * @param id id of the coach
     * @return a dto representing the coach
     * @throws ResourceNotFoundException thrown when the coach does not exist in the database
     */
    public CoachDto findById(UUID id) throws ResourceNotFoundException {
        return this.coachRepository
                .findCoachById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Coach.class, id));
    }

    /**
     * Updates the coach's information.
     *
     * @param id id of the coach to update
     * @param coachDto dto containing values to be placed in the database
     * @return a dto representing the updated coach
     * @throws ResourceNotFoundException thrown when the coach does not exist in the database
     */
    public CoachDto updateCoach(UUID id, UpsertCoachDto coachDto) throws ResourceNotFoundException {
        var coachToUpdate = this.coachRepository
                .findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Coach.class, id));

        coachToUpdate.setName(coachDto.getName());

        return entityToDto(this.coachRepository.save(coachToUpdate));
    }

    /**
     * Creates the coach's entry in the database.
     *
     * @param coachDto dto representing the information about a coach that will be saved in the database
     * @return a dto representing the newly saved coach
     */
    public CoachDto createCoach(UpsertCoachDto coachDto) {
        return entityToDto(
                this.coachRepository
                        .save(new Coach(coachDto.getName()))
        );
    }

    /**
     * Finds all coaches whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the coach
     * @param pageable information about the wanted page
     * @return a page of coaches which match the filter
     */
    public Page<CoachDto> findCoachesByName(String phrase, Pageable pageable) {
        return this.coachRepository.findAllByNameContaining(phrase, pageable);
    }

    /**
     * Marks a coach with the specified id as deleted.
     *
     * @param id id of the coach to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markCoachAsDeleted(UUID id)  {
        return this.coachRepository.markCoachAsDeleted(id);
    }
}
