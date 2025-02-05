package pl.echelon133.competitionservice.competition.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents matches received via `GET /api/matches?matchIds`.
 */
public record CompactMatchDto(
        UUID id,
        String status,
        LocalDateTime statusLastModifiedUTC,
        String result,
        UUID competitionId,
        LocalDateTime startTimeUTC,
        TeamDto homeTeam,
        TeamDto awayTeam,
        ScoreInfoDto halfTimeScoreInfo,
        ScoreInfoDto scoreInfo,
        ScoreInfoDto penaltiesInfo,
        RedCardInfoDto redCardInfo
) {
    public record TeamDto (UUID id, String name, String crestUrl) {}
    public record ScoreInfoDto(int homeGoals, int awayGoals) {}
    public record RedCardInfoDto(int homeRedCards, int awayRedCards) {}
}
