package ml.echelon133.matchservice.match.model;

import javax.persistence.Embeddable;

@Embeddable
public class PenaltiesInfo {

    private Integer homePenalties;
    private Integer awayPenalties;

    public PenaltiesInfo() {
        this.homePenalties = 0;
        this.awayPenalties = 0;
    }

    public PenaltiesInfo(Integer homePenalties, Integer awayPenalties) {
        this.homePenalties = homePenalties;
        this.awayPenalties = awayPenalties;
    }

    public Integer getHomePenalties() {
        return homePenalties;
    }

    public void setHomePenalties(Integer homePenalties) {
        this.homePenalties = homePenalties;
    }

    public Integer getAwayPenalties() {
        return awayPenalties;
    }

    public void setAwayPenalties(Integer awayPenalties) {
        this.awayPenalties = awayPenalties;
    }
}
