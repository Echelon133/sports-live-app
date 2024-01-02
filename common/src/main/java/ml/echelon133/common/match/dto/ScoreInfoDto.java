package ml.echelon133.common.match.dto;

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
