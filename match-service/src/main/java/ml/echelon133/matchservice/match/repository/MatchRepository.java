package ml.echelon133.matchservice.match.repository;

import ml.echelon133.matchservice.match.model.CompactMatchDto;
import ml.echelon133.matchservice.match.model.LineupFormationsDto;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.match.model.MatchDto;
import ml.echelon133.matchservice.team.model.TeamPlayerDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    /**
     * Finds all matches whose ids are in the id list.
     *
     * @param matchIds requested match ids
     * @return a list of matches
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(m.id as varchar) as id, m.status as status, m.result as result, \
                       m.start_time_utc as startTimeUTC, CAST(m.competition_id as varchar) as competitionId, \
                       m.half_time_home_goals as halfTimeHomeGoals, m.half_time_away_goals as halfTimeAwayGoals, \
                       m.home_goals as homeGoals, m.away_goals as awayGoals, \
                       m.home_penalties as homePenalties, m.away_penalties as awayPenalties, \
                       m.home_red_cards as homeRedCards, m.away_red_cards as awayRedCards, \
                       m.status_last_modified_utc as statusLastModifiedUTC, \
                    CAST(ht.id as varchar) as homeTeamId, ht.name as homeTeamName, \
                       ht.crest_url as homeTeamCrestUrl, ht.deleted as homeTeamDeleted, \
                    CAST(at.id as varchar) as awayTeamId, at.name as awayTeamName, \
                       at.crest_url as awayTeamCrestUrl, at.deleted as awayTeamDeleted \
                    FROM match m \
                    JOIN team ht ON m.home_team_id = ht.id \
                    JOIN team at ON m.away_team_id = at.id \
                    WHERE m.deleted = false AND m.id IN :matchIds \
                    """,
            nativeQuery = true
    )
    List<CompactMatchDto> findAllByMatchIds(List<UUID> matchIds);

    /**
     * Finds a non-deleted match with the specified id.
     *
     * @param matchId id of the match
     * @return empty {@link Optional} if the match was not found or is marked as deleted, otherwise contains a {@link MatchDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(m.id as varchar) as id, m.status as status, m.result as result, \
                       m.start_time_utc as startTimeUTC, CAST(m.competition_id as varchar) as competitionId, \
                       m.half_time_home_goals as halfTimeHomeGoals, m.half_time_away_goals as halfTimeAwayGoals, \
                       m.home_goals as homeGoals, m.away_goals as awayGoals, \
                       m.home_penalties as homePenalties, m.away_penalties as awayPenalties, \
                       m.status_last_modified_utc as statusLastModifiedUTC, \
                    CAST(ht.id as varchar) as homeTeamId, ht.name as homeTeamName, \
                       ht.crest_url as homeTeamCrestUrl, ht.deleted as homeTeamDeleted, \
                    CAST(at.id as varchar) as awayTeamId, at.name as awayTeamName, \
                       at.crest_url as awayTeamCrestUrl, at.deleted as awayTeamDeleted, \
                    CAST(v.id as varchar) as venueId, v.name as venueName, \
                       v.capacity as venueCapacity, v.deleted as venueDeleted, \
                    CAST(r.id as varchar) as refereeId, r.name as refereeName, r.deleted as refereeDeleted \
                    FROM match m \
                    JOIN team ht ON m.home_team_id = ht.id \
                    JOIN team at ON m.away_team_id = at.id \
                    JOIN venue v ON m.venue_id = v.id \
                    LEFT JOIN referee r ON m.referee_id = r.id \
                    WHERE m.id = :matchId AND m.deleted = false \
                    """,
            nativeQuery = true
    )
    Optional<MatchDto> findMatchById(UUID matchId);

    /**
     * Marks the match with the specified id as deleted.
     *
     * @param matchId id of the match to be marked as deleted
     * @return count of how many matches have been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE match SET deleted = true WHERE id = :matchId AND deleted = false", nativeQuery = true)
    Integer markMatchAsDeleted(UUID matchId);

    /**
     * Finds all matches that start between the two dates specified in the arguments.
     *
     * @param startUTC start of the search period (in UTC)
     * @param endUTC end of the search period (in UTC)
     * @param pageable information about the wanted page
     * @return a list of matches that start between the two dates
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(m.id as varchar) as id, m.status as status, m.result as result, \
                       m.start_time_utc as startTimeUTC, CAST(m.competition_id as varchar) as competitionId, \
                       m.half_time_home_goals as halfTimeHomeGoals, m.half_time_away_goals as halfTimeAwayGoals, \
                       m.home_goals as homeGoals, m.away_goals as awayGoals, \
                       m.home_penalties as homePenalties, m.away_penalties as awayPenalties, \
                       m.home_red_cards as homeRedCards, m.away_red_cards as awayRedCards, \
                       m.status_last_modified_utc as statusLastModifiedUTC, \
                    CAST(ht.id as varchar) as homeTeamId, ht.name as homeTeamName, \
                       ht.crest_url as homeTeamCrestUrl, ht.deleted as homeTeamDeleted, \
                    CAST(at.id as varchar) as awayTeamId, at.name as awayTeamName, \
                       at.crest_url as awayTeamCrestUrl, at.deleted as awayTeamDeleted \
                    FROM match m \
                    JOIN team ht ON m.home_team_id = ht.id \
                    JOIN team at ON m.away_team_id = at.id \
                    WHERE m.deleted = false AND m.start_time_utc BETWEEN :startUTC AND :endUTC \
                    ORDER BY m.start_time_utc ASC \
                    """,
            nativeQuery = true
    )
    List<CompactMatchDto> findAllBetween(LocalDateTime startUTC, LocalDateTime endUTC, Pageable pageable);

    /**
     * Finds all matches of a team with the specified id, whose status is on the list
     * of accepted statuses.
     *
     * @param teamId id of the team which plays in a match
     * @param acceptedStatuses a list of accepted {@link ml.echelon133.common.match.MatchStatus} values (represented
     *                         as strings). The query will only return matches with statuses that appear on the list.
     * @param pageable information about the wanted page
     * @return a list of matches of the specified team, whose statuses belong to the specified list of statuses
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(m.id as varchar) as id, m.status as status, m.result as result, \
                       m.start_time_utc as startTimeUTC, CAST(m.competition_id as varchar) as competitionId, \
                       m.half_time_home_goals as halfTimeHomeGoals, m.half_time_away_goals as halfTimeAwayGoals, \
                       m.home_goals as homeGoals, m.away_goals as awayGoals, \
                       m.home_penalties as homePenalties, m.away_penalties as awayPenalties, \
                       m.home_red_cards as homeRedCards, m.away_red_cards as awayRedCards, \
                       m.status_last_modified_utc as statusLastModifiedUTC, \
                    CAST(ht.id as varchar) as homeTeamId, ht.name as homeTeamName, \
                       ht.crest_url as homeTeamCrestUrl, ht.deleted as homeTeamDeleted, \
                    CAST(at.id as varchar) as awayTeamId, at.name as awayTeamName, \
                       at.crest_url as awayTeamCrestUrl, at.deleted as awayTeamDeleted \
                    FROM match m \
                    JOIN team ht ON m.home_team_id = ht.id \
                    JOIN team at ON m.away_team_id = at.id \
                    WHERE m.deleted = false \
                    AND (m.home_team_id = :teamId OR m.away_team_id = :teamId) \
                    AND m.status IN :acceptedStatuses \
                    ORDER BY m.start_time_utc ASC \
                    """,
            nativeQuery = true
    )
    List<CompactMatchDto> findAllByTeamIdAndStatuses(UUID teamId, List<String> acceptedStatuses, Pageable pageable);

    /**
     * Finds all non-deleted starting players who are in the home lineup of a match with the specified id.
     *
     * @param matchId id of the match whose starting players need to be found
     * @return a list of starting players of the home side of the match
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(tp.id as varchar) as id, tp.position as position, tp.number as number, \
                    CAST(p.id as varchar) as playerId, p.name as name, p.date_of_birth as dateOfBirth, \
                    p.country_code as countryCode \
                    FROM match m \
                    JOIN lineup l ON m.home_lineup_id = l.id \
                    JOIN starting_player stp ON stp.lineup_id = l.id \
                    JOIN team_player tp ON stp.team_player_id = tp.id \
                    JOIN player p ON tp.player_id = p.id \
                    WHERE m.deleted = false AND m.id = :matchId AND p.deleted = false \
                    """,
            nativeQuery = true
    )
    List<TeamPlayerDto> findHomeStartingPlayersByMatchId(UUID matchId);

    /**
     * Finds all non-deleted substitute players who are in the home lineup of a match with the specified id.
     *
     * @param matchId id of the match whose substitute players need to be found
     * @return a list of substitute players of the home side of the match
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(tp.id as varchar) as id, tp.position as position, tp.number as number, \
                    CAST(p.id as varchar) as playerId, p.name as name, p.date_of_birth as dateOfBirth, \
                    p.country_code as countryCode \
                    FROM match m \
                    JOIN lineup l ON m.home_lineup_id = l.id \
                    JOIN substitute_player sup ON sup.lineup_id = l.id \
                    JOIN team_player tp ON sup.team_player_id = tp.id \
                    JOIN player p ON tp.player_id = p.id \
                    WHERE m.deleted = false AND m.id = :matchId AND p.deleted = false \
                    """,
            nativeQuery = true
    )
    List<TeamPlayerDto> findHomeSubstitutePlayersByMatchId(UUID matchId);

    /**
     * Finds all non-deleted starting players who are in the away lineup of a match with the specified id.
     *
     * @param matchId id of the match whose starting players need to be found
     * @return a list of starting players of the away side of the match
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(tp.id as varchar) as id, tp.position as position, tp.number as number, \
                    CAST(p.id as varchar) as playerId, p.name as name, p.date_of_birth as dateOfBirth, \
                    p.country_code as countryCode \
                    FROM match m \
                    JOIN lineup l ON m.away_lineup_id = l.id \
                    JOIN starting_player stp ON stp.lineup_id = l.id \
                    JOIN team_player tp ON stp.team_player_id = tp.id \
                    JOIN player p ON tp.player_id = p.id \
                    WHERE m.deleted = false AND m.id = :matchId AND p.deleted = false \
                    """,
            nativeQuery = true
    )
    List<TeamPlayerDto> findAwayStartingPlayersByMatchId(UUID matchId);

    /**
     * Finds all non-deleted substitute players who are in the away lineup of a match with the specified id.
     *
     * @param matchId id of the match whose substitute players need to be found
     * @return a list of substitute players of the away side of the match
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(
            value = """
                    SELECT CAST(tp.id as varchar) as id, tp.position as position, tp.number as number, \
                    CAST(p.id as varchar) as playerId, p.name as name, p.date_of_birth as dateOfBirth, \
                    p.country_code as countryCode \
                    FROM match m \
                    JOIN lineup l ON m.away_lineup_id = l.id \
                    JOIN substitute_player sup ON sup.lineup_id = l.id \
                    JOIN team_player tp ON sup.team_player_id = tp.id \
                    JOIN player p ON tp.player_id = p.id \
                    WHERE m.deleted = false AND m.id = :matchId AND p.deleted = false \
                    """,
            nativeQuery = true
    )
    List<TeamPlayerDto> findAwaySubstitutePlayersByMatchId(UUID matchId);

    /**
     * Finds formations of both lineups of a match with the specified id.
     *
     * @param matchId id of the match whose lineup formations need to be found
     * @return an {@link Optional} containing a dto with lineup formations of both teams playing in a match (if the match exists),
     *      otherwise the optional is empty
     */
    @Query(
            value = """
                    SELECT home_lineup.formation as homeFormation, away_lineup.formation as awayFormation \
                    FROM match m \
                    JOIN lineup home_lineup ON m.home_lineup_id = home_lineup.id \
                    JOIN lineup away_lineup ON m.away_lineup_id = away_lineup.id \
                    WHERE m.deleted = false AND m.id = :matchId \
                    """,
            nativeQuery = true
    )
    Optional<LineupFormationsDto> findLineupFormationsByMatchId(UUID matchId);
}
