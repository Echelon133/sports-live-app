package ml.echelon133.common.match.dto;

public interface PenaltiesInfoDto {
    Byte getHomePenalties();
    Byte getAwayPenalties();

    static PenaltiesInfoDto from(Byte homePenalties, Byte awayPenalties) {
        return new PenaltiesInfoDto() {
            @Override
            public Byte getHomePenalties() {
                return homePenalties;
            }

            @Override
            public Byte getAwayPenalties() {
                return awayPenalties;
            }
        };
    }
}
