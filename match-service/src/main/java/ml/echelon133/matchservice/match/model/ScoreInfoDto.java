package ml.echelon133.matchservice.match.model;

import java.io.Serializable;

public interface ScoreInfoDto extends Serializable {
    Integer getHomeGoals();
    Integer getAwayGoals();

    static ScoreInfoDto from(Integer homeGoals, Integer awayGoals) {
        return new ScoreInfoDto() {
            @Override
            public Integer getHomeGoals() {
                return homeGoals;
            }

            @Override
            public Integer getAwayGoals() {
                return awayGoals;
            }
        };
    }
}
