package ml.echelon133.matchservice.match;

import ml.echelon133.matchservice.match.model.UpsertMatchDto;

import java.util.UUID;

public interface TestUpsertMatchDto {
    static UpsertMatchDtoBuilder builder() {
        return new UpsertMatchDtoBuilder();
    }

    class UpsertMatchDtoBuilder {
        private String homeTeamId = UUID.randomUUID().toString();
        private String awayTeamId = UUID.randomUUID().toString();
        private String startTimeUTC = "2023/01/01 19:00";
        private String venueId = UUID.randomUUID().toString();
        private String refereeId = UUID.randomUUID().toString();
        private String competitionId = UUID.randomUUID().toString();

        private UpsertMatchDtoBuilder() {}

        public UpsertMatchDtoBuilder homeTeamId(String homeTeamId) {
            this.homeTeamId = homeTeamId;
            return this;
        }

        public UpsertMatchDtoBuilder awayTeamId(String awayTeamId) {
            this.awayTeamId = awayTeamId;
            return this;
        }

        public UpsertMatchDtoBuilder startTimeUTC(String startTimeUTC) {
            this.startTimeUTC = startTimeUTC;
            return this;
        }

        public UpsertMatchDtoBuilder venueId(String venueId) {
            this.venueId = venueId;
            return this;
        }

        public UpsertMatchDtoBuilder refereeId(String refereeId) {
            this.refereeId = refereeId;
            return this;
        }

        public UpsertMatchDtoBuilder competitionId(String competitionId) {
            this.competitionId = competitionId;
            return this;
        }

        public UpsertMatchDto build() {
            var upsertMatchDto = new UpsertMatchDto();
            upsertMatchDto.setHomeTeamId(homeTeamId);
            upsertMatchDto.setAwayTeamId(awayTeamId);
            upsertMatchDto.setStartTimeUTC(startTimeUTC);
            upsertMatchDto.setVenueId(venueId);
            upsertMatchDto.setRefereeId(refereeId);
            upsertMatchDto.setCompetitionId(competitionId);
            return upsertMatchDto;
        }
    }
}
