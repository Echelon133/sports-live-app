package pl.echelon133.competitionservice.competition;

import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;

public interface TestUpsertCompetitionDto {

    static UpsertCompetitionDtoBuilder builder() {
        return new UpsertCompetitionDtoBuilder();
    }

    class UpsertCompetitionDtoBuilder {

        private String name = "Test Competition";
        private String season = "2023/24";
        private String logoUrl = "http://test.com/logo.png";
        private UpsertCompetitionDto.UpsertLeaguePhaseDto leaguePhase = null;
        private UpsertCompetitionDto.UpsertKnockoutPhaseDto knockoutPhase = null;
        private boolean pinned;

        private UpsertCompetitionDtoBuilder() {}

        public UpsertCompetitionDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UpsertCompetitionDtoBuilder season(String season) {
            this.season = season;
            return this;
        }

        public UpsertCompetitionDtoBuilder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public UpsertCompetitionDtoBuilder leaguePhase(UpsertCompetitionDto.UpsertLeaguePhaseDto leaguePhase) {
            this.leaguePhase = leaguePhase;
            return this;
        }

        public UpsertCompetitionDtoBuilder knockoutPhase(UpsertCompetitionDto.UpsertKnockoutPhaseDto knockoutPhase) {
            this.knockoutPhase = knockoutPhase;
            return this;
        }

        public UpsertCompetitionDtoBuilder pinned(boolean pinned) {
            this.pinned = pinned;
            return this;
        }

        public UpsertCompetitionDto build() {
            return new UpsertCompetitionDto(name, season, logoUrl, leaguePhase, knockoutPhase, pinned);
        }
    }
}
