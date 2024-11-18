package ml.echelon133.matchservice.match.model;

public interface LineupFormationsDto {
    String getHomeFormation();
    String getAwayFormation();

    static LineupFormationsDto from(String homeFormation, String awayFormation) {
        return new LineupFormationsDto() {
            @Override
            public String getHomeFormation() {
                return homeFormation;
            }

            @Override
            public String getAwayFormation() {
                return awayFormation;
            }
        };
    }
}
