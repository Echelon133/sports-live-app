package pl.echelon133.competitionservice.competition.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record UnassignedMatchDto(
        UUID id,
        String status,
        String result,
        LocalDateTime startTimeUTC,
        TeamDto homeTeam,
        TeamDto awayTeam
) {
    record TeamDto (UUID id, String name, String crestUrl) {}
}
