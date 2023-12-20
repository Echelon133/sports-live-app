package ml.echelon133.matchservice.match.model;

import javax.persistence.Embeddable;

@Embeddable
public class ScoreInfo {

    private Integer homeGoals;
    private Integer awayGoals;

    public ScoreInfo() {
        this.homeGoals = 0;
        this.awayGoals = 0;
    }

    public ScoreInfo(Integer homeGoals, Integer awayGoals) {
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    public Integer getHomeGoals() {
        return homeGoals;
    }

    public void setHomeGoals(Integer homeGoals) {
        this.homeGoals = homeGoals;
    }

    public Integer getAwayGoals() {
        return awayGoals;
    }

    public void setAwayGoals(Integer awayGoals) {
        this.awayGoals = awayGoals;
    }

    public void incrementHomeGoals() {
        ++this.homeGoals;
    }

    public void incrementAwayGoals() {
        ++this.awayGoals;
    }
}
