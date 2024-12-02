package ml.echelon133.matchservice.match.model;

import jakarta.validation.constraints.NotNull;
import ml.echelon133.common.constraints.LocalDateTimeFormat;
import ml.echelon133.matchservice.match.model.constraints.TeamIdsDifferent;
import ml.echelon133.matchservice.referee.constraints.RefereeExists;
import ml.echelon133.matchservice.team.constraints.TeamExists;
import ml.echelon133.matchservice.venue.constraints.VenueExists;
import org.hibernate.validator.constraints.UUID;

@TeamIdsDifferent
public record UpsertMatchDto(
        @NotNull @TeamExists String homeTeamId,
        @NotNull @TeamExists String awayTeamId,
        @NotNull @LocalDateTimeFormat String startTimeUTC,
        @NotNull @VenueExists String venueId,
        // this is optional, since at the time of creation of most of the matches, the referee is unknown
        @RefereeExists String refereeId,
        @NotNull @UUID String competitionId
) {
}
