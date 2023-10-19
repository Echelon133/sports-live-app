package ml.echelon133.matchservice.match.model;

import ml.echelon133.common.constraints.UUID;
import ml.echelon133.matchservice.match.model.constraints.TeamIdsDifferent;
import ml.echelon133.common.constraints.LocalDateTimeFormat;

import javax.validation.constraints.NotNull;

@TeamIdsDifferent
public class UpsertMatchDto {

    @NotNull(message = "field has to be provided")
    @UUID
    private String homeTeamId;

    @NotNull(message = "field has to be provided")
    @UUID
    private String awayTeamId;

    @NotNull(message = "field has to be provided")
    @LocalDateTimeFormat
    private String startTimeUTC;

    @NotNull(message = "field has to be provided")
    @UUID
    private String venueId;

    // this is optional, since at the time of creation of most of the matches, the referee is unknown
    @UUID
    private String refereeId;

    @NotNull(message = "field has to be provided")
    @UUID
    private String competitionId;

    public UpsertMatchDto() {}

    public String getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(String homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public String getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(String awayTeamId) {
        this.awayTeamId = awayTeamId;
    }

    public String getStartTimeUTC() {
        return startTimeUTC;
    }

    public void setStartTimeUTC(String startTimeUTC) {
        this.startTimeUTC = startTimeUTC;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getRefereeId() {
        return refereeId;
    }

    public void setRefereeId(String refereeId) {
        this.refereeId = refereeId;
    }

    public String getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(String competitionId) {
        this.competitionId = competitionId;
    }
}
