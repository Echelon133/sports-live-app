package pl.echelon133.competitionservice.competition.repository;

import ml.echelon133.common.competition.dto.CompetitionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.PlayerStatsDto;

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

    /**
     * Finds all competition-specific statistics of players who play in the specified competition.
     * Orders statistics by goals, then assists of players.
     *
     * @param competitionId id of the competition of which the statistics will be fetched
     * @param pageable information about the wanted page
     * @return a page containing player statistics
     */
    @Query(
            value = "SELECT CAST(ps.player_id as varchar) as playerId, CAST(ps.team_id as varchar) as teamId, " +
                    "ps.name as name, ps.goals as goals, ps.assists as assists, ps.yellow_cards as yellowCards, ps.red_cards as redCards " +
                    "FROM player_stats ps JOIN competition_player_stats cps ON cps.player_stats_id = ps.id " +
                    "WHERE cps.competition_id = :competitionId " +
                    "ORDER BY ps.goals DESC, ps.assists DESC",
            countQuery =
                    "SELECT (*) " +
                    "FROM player_stats ps JOIN competition_player_stats cps ON cps.player_stats_id = ps.id " +
                    "WHERE cps.competition_id = :competitionId ",
            nativeQuery = true
    )
    Page<PlayerStatsDto> findPlayerStats(UUID competitionId, Pageable pageable);
}
