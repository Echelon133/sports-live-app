package ml.echelon133.matchservice.match.model;

import ml.echelon133.common.constraints.LocalDateTimeFormat;
import ml.echelon133.common.constraints.UUID;
import ml.echelon133.matchservice.match.model.constraints.TeamIdsDifferent;

import javax.validation.constraints.NotNull;

@TeamIdsDifferent
public class UpsertMatchDto {

    @NotNull
    @UUID
    private String homeTeamId;

    @NotNull
    @UUID
    private String awayTeamId;

    @NotNull
    @LocalDateTimeFormat
    private String startTimeUTC;

    @NotNull
    @UUID
    private String venueId;

    // this is optional, since at the time of creation of most of the matches, the referee is unknown
    @UUID
    private String refereeId;

    @NotNull
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
