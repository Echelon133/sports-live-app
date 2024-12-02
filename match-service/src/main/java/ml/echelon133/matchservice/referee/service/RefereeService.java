package ml.echelon133.matchservice.referee.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.referee.model.RefereeDto;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.referee.model.UpsertRefereeDto;
import ml.echelon133.matchservice.referee.repository.RefereeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.UUID;

@Service
@Transactional
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
        return refereeRepository
                .findRefereeById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Referee.class, id));
    }

    /**
     * Returns the entity representing a referee with the specified id.
     * @param id id of the referee's entity
     * @return referee's entity
     * @throws ResourceNotFoundException thrown when the referee does not exist in the database or is deleted
     */
    public Referee findEntityById(UUID id) throws ResourceNotFoundException {
        return refereeRepository
                .findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Referee.class, id));
    }

    /**
     * Updates the referee's information.
     *
     * The values in {@link UpsertRefereeDto} have to be pre-validated before being used here, otherwise
     * incorrect data will be placed into the database.
     *
     * @param id id of the referee to update
     * @param refereeDto dto containing updated information about the referee
     * @return a dto representing the updated referee
     * @throws ResourceNotFoundException thrown when the referee does not exist in the database
     */
    public RefereeDto updateReferee(UUID id, UpsertRefereeDto refereeDto) throws ResourceNotFoundException {
        var refereeToUpdate = refereeRepository
                .findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Referee.class, id));

        refereeToUpdate.setName(refereeDto.name());

        return entityToDto(refereeRepository.save(refereeToUpdate));
    }

    /**
     * Creates the referee's entry in the database.
     *
     * The values in {@link UpsertRefereeDto} have to be pre-validated before being used here, otherwise
     * incorrect data will be placed into the database.
     *
     * @param refereeDto dto representing the information about a referee that will be saved in the database
     * @return a dto representing the newly saved referee
     */
    public RefereeDto createReferee(UpsertRefereeDto refereeDto) {
        return entityToDto(
                refereeRepository
                        .save(new Referee(refereeDto.name()))
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
        return refereeRepository.findAllByNameContaining(phrase, pageable);
    }

    /**
     * Marks a referee with the specified id as deleted.
     *
     * @param id id of the referee to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markRefereeAsDeleted(UUID id)  {
        return refereeRepository.markRefereeAsDeleted(id);
    }
}
