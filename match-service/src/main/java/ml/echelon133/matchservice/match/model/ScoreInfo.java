package ml.echelon133.matchservice.match.model;

import jakarta.persistence.Embeddable;

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

    public static ScoreInfo of(Integer homeGoals, Integer awayGoals) {
        return new ScoreInfo(homeGoals, awayGoals);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoreInfo scoreInfo = (ScoreInfo) o;

        if (!homeGoals.equals(scoreInfo.homeGoals)) return false;
        return awayGoals.equals(scoreInfo.awayGoals);
    }

    @Override
    public int hashCode() {
        int result = homeGoals.hashCode();
        result = 31 * result + awayGoals.hashCode();
        return result;
    }
}
