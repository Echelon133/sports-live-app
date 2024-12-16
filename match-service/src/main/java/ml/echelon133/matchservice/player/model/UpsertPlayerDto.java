package ml.echelon133.matchservice.player.model;

import ml.echelon133.common.constraints.CountryCodeFormat;
import ml.echelon133.common.constraints.LocalDateFormat;
import ml.echelon133.matchservice.player.model.constraints.PositionValue;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotNull;

public record UpsertPlayerDto(
        @NotNull @Length(min = 1, max = 200) String name,
        @NotNull @CountryCodeFormat String countryCode,
        @NotNull @PositionValue String position,
        @NotNull @LocalDateFormat String dateOfBirth
) {}
