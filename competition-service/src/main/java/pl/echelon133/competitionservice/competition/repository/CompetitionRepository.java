package pl.echelon133.competitionservice.competition.repository;

import ml.echelon133.common.competition.dto.CompetitionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pl.echelon133.competitionservice.competition.model.Competition;

import java.util.Optional;
import java.util.UUID;

public interface CompetitionRepository extends JpaRepository<Competition, UUID> {

    /**
     * Finds a non-deleted competition with the specified id.
     *
     * @param competitionId id of the competition
     * @return empty {@link Optional} if the competition was not found or is marked as deleted, otherwise contains a {@link CompetitionDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(c.id as varchar) as id, c.name as name, c.season as season, c.logo_url as logoUrl " +
                    "FROM competition c " +
                    "WHERE c.id = :competitionId AND c.deleted = false",
            nativeQuery = true
    )
    Optional<CompetitionDto> findCompetitionById(UUID competitionId);

    /**
     * Marks the competition with the specified id as deleted.
     *
     * @param competitionId id of the competition to be marked as deleted
     * @return count of how many competitions have been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE competition SET deleted = true WHERE id = :competitionId AND deleted = false", nativeQuery = true)
    Integer markCompetitionAsDeleted(UUID competitionId);

    /**
     * Finds all competitions whose names contain a certain phrase.
     *
     * @param phrase phrase (case-insensitive) which has to appear in the name
     * @param pageable information about the wanted page
     * @return a page containing all competitions whose names contain the phrase
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = "SELECT CAST(c.id as varchar) as id, c.name as name, c.season as season, c.logo_url as logoUrl " +
                    "FROM competition c " +
                    "WHERE LOWER(c.name) LIKE '%' || LOWER(:phrase) || '%' AND c.deleted = false",
            countQuery = "SELECT COUNT(*) FROM competition WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true
    )
    Page<CompetitionDto> findAllByNameContaining(String phrase, Pageable pageable);
}
