package ml.echelon133.common.match.dto;

public interface ScoreInfoDto {
    Byte getHomeGoals();
    Byte getAwayGoals();

    static ScoreInfoDto from(Byte homeGoals, Byte awayGoals) {
        return new ScoreInfoDto() {
            @Override
            public Byte getHomeGoals() {
                return homeGoals;
            }

            @Override
            public Byte getAwayGoals() {
                return awayGoals;
            }
        };
    }
}
