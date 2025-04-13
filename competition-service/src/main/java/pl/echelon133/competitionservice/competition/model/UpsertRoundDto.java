package pl.echelon133.competitionservice.competition.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpsertRoundDto(
        @NotNull
        @Size(min = 1, max = 18, message = "expected between {min} and {max} matches")
        List<UUID> matchIds
) {
}
