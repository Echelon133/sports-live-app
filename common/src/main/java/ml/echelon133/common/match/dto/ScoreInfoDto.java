package ml.echelon133.common.match.dto;

public interface ScoreInfoDto {
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
