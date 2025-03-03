package ml.echelon133.matchservice.match.model;

import java.util.UUID;

public record CompetitionDto(
        UUID id,
        String name,
        String season,
        String logoUrl,
        boolean leaguePhase,
        boolean knockoutPhase
) {
}
