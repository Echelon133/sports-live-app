package ml.echelon133.matchservice.coach.repository;

import ml.echelon133.matchservice.coach.model.CoachDto;
import ml.echelon133.matchservice.coach.model.Coach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CoachRepository extends JpaRepository<Coach, UUID> {
    boolean existsByIdAndDeletedFalse(UUID uuid);

    /**
     * Finds a non-deleted coach with the specified id.
     *
     * @param id id of the coach
     * @return empty {@link Optional} if the coach was not found or is marked as deleted, otherwise contains a {@link CoachDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(id as varchar) as id, name FROM coach WHERE deleted = false AND id = ?1",
            nativeQuery = true
    )
    Optional<CoachDto> findCoachById(UUID id);

    /**
     * Marks the coach with the specified id as deleted.
     *
     * @param id id of the coach to be marked as deleted
     * @return count of how many coaches had been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE coach SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    Integer markCoachAsDeleted(UUID id);

    /**
     * Finds all coaches which contain a certain phrase in their name.
     *
     * @param phrase phrase (case-insensitive) which has to appear in the name
     * @param pageable information about the wanted page
     * @return a page containing all coaches whose names contain the phrase
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(id as varchar) as id, name " +
                    "FROM coach WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            countQuery = "SELECT COUNT(*) FROM coach WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true
    )
    Page<CoachDto> findAllByNameContaining(String phrase, Pageable pageable);
}
