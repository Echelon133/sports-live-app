package ml.echelon133.matchservice.match.model;

import ml.echelon133.common.entity.BaseEntity;
import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.referee.model.Referee;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.venue.model.Venue;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        indexes = {
                @Index(columnList = "home_team_id", name = "home_team_id_index"),
                @Index(columnList = "away_team_id", name = "away_team_id_index"),
                @Index(columnList = "competition_id", name = "competition_id_index"),
                @Index(columnList = "result", name = "result_index"),
        }
)
public class Match extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MatchStatus status;

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "start_time_utc", nullable = false)
    private LocalDateTime startTimeUTC;

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    // this is optional, since at the time of creation of most of the matches, the referee is unknown
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "referee_id")
    private Referee referee;

    @Column(name = "competition_id")
    private UUID competitionId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="homeGoals", column = @Column(name = "half_time_home_goals")),
            @AttributeOverride(name="awayGoals", column = @Column(name = "half_time_away_goals"))
    })
    private ScoreInfo halfTimeScoreInfo;

    @Embedded
    private ScoreInfo scoreInfo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="homeGoals", column = @Column(name = "home_penalties")),
            @AttributeOverride(name="awayGoals", column = @Column(name = "away_penalties"))
    })
    private ScoreInfo penaltiesInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MatchResult result;

    public Match() {
        this.status = MatchStatus.NOT_STARTED;
        this.halfTimeScoreInfo = new ScoreInfo();
        this.scoreInfo = new ScoreInfo();
        this.penaltiesInfo = new ScoreInfo();
        this.result = MatchResult.NONE;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(Team homeTeam) {
        this.homeTeam = homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(Team awayTeam) {
        this.awayTeam = awayTeam;
    }

    public LocalDateTime getStartTimeUTC() {
        return startTimeUTC;
    }

    public void setStartTimeUTC(LocalDateTime startTimeUTC) {
        this.startTimeUTC = startTimeUTC;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public Referee getReferee() {
        return referee;
    }

    public void setReferee(Referee referee) {
        this.referee = referee;
    }

    public UUID getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(UUID competitionId) {
        this.competitionId = competitionId;
    }

    public ScoreInfo getHalfTimeScoreInfo() {
        return halfTimeScoreInfo;
    }

    public void setHalfTimeScoreInfo(ScoreInfo halfTimeScoreInfo) {
        this.halfTimeScoreInfo = halfTimeScoreInfo;
    }

    public ScoreInfo getScoreInfo() {
        return scoreInfo;
    }

    public void setScoreInfo(ScoreInfo scoreInfo) {
        this.scoreInfo = scoreInfo;
    }

    public ScoreInfo getPenaltiesInfo() {
        return penaltiesInfo;
    }

    public void setPenaltiesInfo(ScoreInfo penaltiesInfo) {
        this.penaltiesInfo = penaltiesInfo;
    }

    public MatchResult getResult() {
        return result;
    }

    public void setResult(MatchResult result) {
        this.result = result;
    }
}
