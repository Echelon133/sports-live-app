package pl.echelon133.competitionservice.competition.model;

import java.util.UUID;

public record TeamDetailsDto(
        UUID id,
        String name,
        String crestUrl,
        String countryCode,
        CoachDto coach
) {
    public TeamDetailsDto(UUID id, String name, String crestUrl) {
        this(id, name, crestUrl, null, null);
    }

    public record CoachDto(UUID id, String name) {}
}
