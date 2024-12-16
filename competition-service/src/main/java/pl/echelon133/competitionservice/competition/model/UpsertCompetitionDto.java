package pl.echelon133.competitionservice.competition.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.UUID;
import pl.echelon133.competitionservice.competition.model.constraints.LegendPositionsInRange;
import pl.echelon133.competitionservice.competition.model.constraints.PositionsUniqueInLegend;
import pl.echelon133.competitionservice.competition.model.constraints.SentimentValue;
import pl.echelon133.competitionservice.competition.model.constraints.TeamsUniqueInGroups;

import java.util.List;
import java.util.Set;

@LegendPositionsInRange
public record UpsertCompetitionDto(
    @NotNull @Length(min = 1, max = 50) String name,
    @NotNull @Length(min = 1, max = 30) String season,
    @NotNull @URL @Length(min = 15, max = 500) String logoUrl,
    @Size(min = 1, max = 10) @TeamsUniqueInGroups List<@Valid UpsertGroupDto> groups,
    @Size(max = 6) @PositionsUniqueInLegend List<@Valid UpsertLegendDto> legend,
    boolean pinned
) {
    public record UpsertGroupDto(
        @NotNull @Length(max = 50) String name,
        @Size(min = 2, max = 36) List<@UUID String> teams
    ) {}

    public record UpsertLegendDto(
        @Size(min = 1, max = 16) Set<Integer> positions,
        @NotNull @Length(min = 1, max = 200) String context,
        @NotNull @SentimentValue String sentiment
    ) {}
}
