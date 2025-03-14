package pl.echelon133.competitionservice.competition.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.CompetitionDto;
import pl.echelon133.competitionservice.competition.model.LabeledMatch;
import pl.echelon133.competitionservice.competition.model.PlayerStatsDto;

import java.util.List;
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
            value = """
                    SELECT CAST(c.id as varchar) as id, c.name as name, c.season as season, c.logo_url as logoUrl, \
                    c.league_phase_id IS NOT NULL as leaguePhase, c.knockout_phase_id IS NOT NULL as knockoutPhase, \
                    CASE WHEN c.league_phase_id IS NULL THEN 0 ELSE lp.max_rounds END as maxRounds \
                    FROM competition c \
                    LEFT JOIN league_phase lp ON lp.id = c.league_phase_id \
                    WHERE c.id = :competitionId AND c.deleted = false \
                    """,
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
            value = """
                    SELECT CAST(c.id as varchar) as id, c.name as name, c.season as season, c.logo_url as logoUrl, \
                    c.league_phase_id IS NOT NULL as leaguePhase, c.knockout_phase_id IS NOT NULL as knockoutPhase, \
                    CASE WHEN c.league_phase_id IS NULL THEN 0 ELSE lp.max_rounds END as maxRounds \
                    FROM competition c \
                    LEFT JOIN league_phase lp ON lp.id = c.league_phase_id \
                    WHERE LOWER(c.name) LIKE '%' || LOWER(:phrase) || '%' AND c.deleted = false \
                    """,
            countQuery = "SELECT COUNT(*) FROM competition WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true
    )
    Page<CompetitionDto> findAllByNameContaining(String phrase, Pageable pageable);

    /**
     * Finds all non-deleted competitions which are marked as <i>pinned</i>.
     *
     * @return a list of non-deleted competitions which are marked as "pinned"
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(c.id as varchar) as id, c.name as name, c.season as season, c.logo_url as logoUrl, \
                    c.league_phase_id IS NOT NULL as leaguePhase, c.knockout_phase_id IS NOT NULL as knockoutPhase, \
                    CASE WHEN c.league_phase_id IS NULL THEN 0 ELSE lp.max_rounds END as maxRounds \
                    FROM competition c \
                    LEFT JOIN league_phase lp ON lp.id = c.league_phase_id \
                    WHERE c.pinned = true AND c.deleted = false \
                    """,
            nativeQuery = true
    )
    List<CompetitionDto> findAllPinned();

    /**
     * Finds all competition-specific statistics of players who play in the specified competition.
     * Orders statistics by goals, then assists of players.
     *
     * @param competitionId id of the competition of which the statistics will be fetched
     * @param pageable information about the wanted page
     * @return a page containing player statistics
     */
    @Query(
            value = """
                    SELECT CAST(ps.player_id as varchar) as playerId, CAST(ps.team_id as varchar) as teamId, \
                    ps.name as name, ps.goals as goals, ps.assists as assists, ps.yellow_cards as yellowCards, ps.red_cards as redCards \
                    FROM player_stats ps \
                    WHERE ps.competition_id = :competitionId \
                    ORDER BY ps.goals DESC, ps.assists DESC \
                    """,
            countQuery = "SELECT COUNT(*) FROM player_stats ps WHERE ps.competition_id = :competitionId",
            nativeQuery = true
    )
    Page<PlayerStatsDto> findPlayerStats(UUID competitionId, Pageable pageable);

    /**
     * Finds all matchIds of matches which happen in the specified competition (filtered by their 'finished' status) and
     * labels them.
     * <p>
     *     For matches which are from a league phase - which are assigned to a round - the label is the number of that
     *     round.
     *     For matches which are from a knockout phase - which are assigned to a stage - the label is the name of that
     *     stage (i.e. ROUND_OF_16, FINAL, etc.)
     * </p>
     *
     * @param competitionId id of the competition whose matches will be fetched
     * @param finished whether fetched matches have to be finished
     * @return a list of labeled matchIds
     */
    @Query(
            value = """
                    SELECT s.stage as label, CAST(cm.match_id as varchar) as matchId, cm.date_created as dateCreated \
                    FROM competition_match cm \
                    JOIN knockout_slot_legs ksl ON cm.id = ksl.legs_id \
                    JOIN stage_slots ss ON ksl."knockout_slot$taken_id" = ss.slots_id \
                    JOIN stage s ON ss.stage_id = s.id \
                    JOIN knockout_phase_stages kps ON s.id = kps.stages_id \
                    JOIN competition c ON kps.knockout_phase_id = c.knockout_phase_id \
                    WHERE c.id = :competitionId AND cm.finished = :finished \
                    UNION \
                    SELECT CAST(ls.round as varchar) as label, CAST(cm.match_id as varchar) as matchId, cm.date_created as dateCreated \
                    FROM league_slot ls \
                    JOIN competition_match cm ON ls.match_id = cm.id \
                    WHERE ls.competition_id = :competitionId AND cm.finished = :finished \
                    ORDER BY dateCreated ASC \
                    """,
            countQuery =
                    """
                    SELECT COUNT(*) FROM (
                        SELECT s.stage as label, cm.match_id as matchId \
                        FROM competition_match cm \
                        JOIN knockout_slot_legs ksl ON cm.id = ksl.legs_id \
                        JOIN stage_slots ss ON ksl."knockout_slot$taken_id" = ss.slots_id \
                        JOIN stage s ON ss.stage_id = s.id \
                        JOIN knockout_phase_stages kps ON s.id = kps.stages_id \
                        JOIN competition c ON kps.knockout_phase_id = c.knockout_phase_id \
                        WHERE c.id = :competitionId AND cm.finished = :finished \
                        UNION \
                        SELECT CAST(ls.round as varchar) as label, cm.match_id as matchId \
                        FROM league_slot ls \
                        JOIN competition_match cm ON ls.match_id = cm.id \
                        WHERE ls.competition_id = :competitionId AND cm.finished = :finished \
                    ) AS labeled_matches
                    """,
            nativeQuery = true
    )
    Page<LabeledMatch> findMatchesLabeledByRoundOrStage(UUID competitionId, boolean finished, Pageable pageable);
}
