package ml.echelon133.matchservice.match.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ScoreInfo {

    @Column()
    private Byte homeGoals;
    private Byte awayGoals;

    public ScoreInfo() {
        this.homeGoals = 0;
        this.awayGoals = 0;
    }

    public ScoreInfo(Byte homeGoals, Byte awayGoals) {
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    public Byte getHomeGoals() {
        return homeGoals;
    }

    public void setHomeGoals(Byte homeGoals) {
        this.homeGoals = homeGoals;
    }

    public Byte getAwayGoals() {
        return awayGoals;
    }

    public void setAwayGoals(Byte awayGoals) {
        this.awayGoals = awayGoals;
    }
}
