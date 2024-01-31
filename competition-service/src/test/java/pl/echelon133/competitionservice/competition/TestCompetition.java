package pl.echelon133.competitionservice.competition;

import pl.echelon133.competitionservice.competition.model.Competition;

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
        private boolean deleted = false;

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

        public CompetitionBuilder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Competition build() {
            var competition = new Competition(name, season, logoUrl);
            competition.setId(id);
            competition.setDeleted(deleted);
            return competition;
        }
    }
}
