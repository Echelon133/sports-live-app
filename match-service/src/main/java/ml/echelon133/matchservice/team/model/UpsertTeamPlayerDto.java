package ml.echelon133.matchservice.team.model;

import ml.echelon133.matchservice.player.constraints.PlayerExists;
import ml.echelon133.matchservice.player.model.constraints.PositionValue;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotNull;

public record UpsertTeamPlayerDto(
    @NotNull @PlayerExists String playerId,
    @NotNull @PositionValue String position,
    @NotNull @Range(min = 1, max = 99, message = "expected number between {min} and {max}") Integer number
) {}
