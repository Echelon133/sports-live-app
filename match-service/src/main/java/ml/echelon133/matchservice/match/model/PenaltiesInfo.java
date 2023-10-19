package ml.echelon133.matchservice.match.model;

import javax.persistence.Embeddable;

@Embeddable
public class PenaltiesInfo {

    private Byte homePenalties;
    private Byte awayPenalties;

    public PenaltiesInfo() {
        this.homePenalties = 0;
        this.awayPenalties = 0;
    }

    public PenaltiesInfo(Byte homePenalties, Byte awayPenalties) {
        this.homePenalties = homePenalties;
        this.awayPenalties = awayPenalties;
    }

    public Byte getHomePenalties() {
        return homePenalties;
    }

    public void setHomePenalties(Byte homePenalties) {
        this.homePenalties = homePenalties;
    }

    public Byte getAwayPenalties() {
        return awayPenalties;
    }

    public void setAwayPenalties(Byte awayPenalties) {
        this.awayPenalties = awayPenalties;
    }
}
