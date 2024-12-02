package ml.echelon133.matchservice.team.model;

import ml.echelon133.common.constraints.CountryCodeFormat;
import ml.echelon133.matchservice.coach.constraints.CoachExists;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotNull;

public record UpsertTeamDto(
        @NotNull @Length(min = 1, max = 200) String name,
        @NotNull @URL String crestUrl,
        @NotNull @CountryCodeFormat String countryCode,
        @NotNull @CoachExists String coachId
) {}
