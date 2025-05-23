package pl.echelon133.competitionservice.competition;

import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.KnockoutPhase;
import pl.echelon133.competitionservice.competition.model.LeaguePhase;

import java.util.List;
import java.util.UUID;

public interface TestCompetition {

    static CompetitionBuilder builder() {
        return new CompetitionBuilder();
    }

    class CompetitionBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Test Competition";
        private String season = "2023/24";
        private String logoUrl = "http://test-logo.com/image.png";
        private LeaguePhase leaguePhase = new LeaguePhase(List.of(), List.of(), 38);
        private KnockoutPhase knockoutPhase = new KnockoutPhase(List.of());
        private boolean deleted = false;
        private boolean pinned = false;

        private CompetitionBuilder() {}

        public CompetitionBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public CompetitionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CompetitionBuilder season(String season) {
            this.season = season;
            return this;
        }

        public CompetitionBuilder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public CompetitionBuilder leaguePhase(LeaguePhase leaguePhase) {
            this.leaguePhase = leaguePhase;
            return this;
        }

        public CompetitionBuilder knockoutPhase(KnockoutPhase knockoutPhase) {
            this.knockoutPhase = knockoutPhase;
            return this;
        }

        public CompetitionBuilder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public CompetitionBuilder pinned(boolean pinned) {
            this.pinned = pinned;
            return this;
        }

        public Competition build() {
            var competition = new Competition(name, season, logoUrl);
            competition.setPinned(pinned);
            competition.setLeaguePhase(leaguePhase);
            competition.setKnockoutPhase(knockoutPhase);
            competition.setId(id);
            competition.setDeleted(deleted);
            return competition;
        }
    }
}
