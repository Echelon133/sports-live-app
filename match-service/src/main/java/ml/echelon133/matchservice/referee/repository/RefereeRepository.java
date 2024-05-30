package ml.echelon133.matchservice.referee.repository;

import ml.echelon133.matchservice.referee.model.RefereeDto;
import ml.echelon133.matchservice.referee.model.Referee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefereeRepository extends JpaRepository<Referee, UUID> {
    boolean existsByIdAndDeletedFalse(UUID refereeId);

    /**
     * Finds a non-deleted referee with the specified id.
     *
     * @param id id of the referee
     * @return empty {@link Optional} if the referee was not found or is marked as deleted, otherwise contains a {@link RefereeDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(id as varchar) as id, name FROM referee WHERE deleted = false AND id = ?1",
            nativeQuery = true
    )
    Optional<RefereeDto> findRefereeById(UUID id);

    /**
     * Marks the referee with the specified id as deleted.
     *
     * @param id id of the referee to be marked as deleted
     * @return count of how many referees had been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE referee SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    Integer markRefereeAsDeleted(UUID id);

    /**
     * Finds all referees which contain a certain phrase in their name.
     *
     * @param phrase phrase (case-insensitive) which has to appear in the name
     * @param pageable information about the wanted page
     * @return a page containing all referees whose names contain the phrase
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(id as varchar) as id, name " +
                    "FROM referee WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            countQuery = "SELECT COUNT(*) FROM referee WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true
    )
    Page<RefereeDto> findAllByNameContaining(String phrase, Pageable pageable);
}
