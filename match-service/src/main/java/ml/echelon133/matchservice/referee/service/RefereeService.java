package ml.echelon133.matchservice.referee.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.referee.dto.RefereeDto;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.referee.repository.RefereeRepository;
import ml.echelon133.matchservice.referee.model.UpsertRefereeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefereeService {

    private final RefereeRepository refereeRepository;

    @Autowired
    public RefereeService(RefereeRepository refereeRepository) {
        this.refereeRepository = refereeRepository;
    }

    private static RefereeDto entityToDto(Referee referee) {
        return RefereeDto.from(referee.getId(), referee.getName());
    }

    /**
     * Returns the information about the referee with specified id.
     *
     * @param id id of the referee
     * @return a dto representing the referee
     * @throws ResourceNotFoundException thrown when the referee does not exist in the database
     */
    public RefereeDto findById(UUID id) throws ResourceNotFoundException {
        return this.refereeRepository
                .findRefereeById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Referee.class, id));
    }

    /**
     * Updates the referee's information.
     *
     * @param id id of the referee to update
     * @param refereeDto dto containing values to be placed in the database
     * @return a dto representing the updated referee
     * @throws ResourceNotFoundException thrown when the referee does not exist in the database
     */
    public RefereeDto updateReferee(UUID id, UpsertRefereeDto refereeDto) throws ResourceNotFoundException {
        var refereeToUpdate = this.refereeRepository
                .findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Referee.class, id));

        refereeToUpdate.setName(refereeDto.getName());

        return entityToDto(this.refereeRepository.save(refereeToUpdate));
    }

    /**
     * Creates the referee's entry in the database.
     *
     * @param refereeDto dto representing the information about a referee that will be saved in the database
     * @return a dto representing the newly saved referee
     */
    public RefereeDto createReferee(UpsertRefereeDto refereeDto) {
        return entityToDto(
                this.refereeRepository
                        .save(new Referee(refereeDto.getName()))
        );
    }

    /**
     * Finds all referees whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the referee
     * @param pageable information about the wanted page
     * @return a page of referees which match the filter
     */
    public Page<RefereeDto> findRefereesByName(String phrase, Pageable pageable) {
        return this.refereeRepository.findAllByNameContaining(phrase, pageable);
    }

    /**
     * Marks a referee with the specified id as deleted.
     *
     * @param id id of the referee to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markRefereeAsDeleted(UUID id)  {
        return this.refereeRepository.markRefereeAsDeleted(id);
    }
}
