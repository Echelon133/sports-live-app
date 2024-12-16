package pl.echelon133.competitionservice.competition;

import pl.echelon133.competitionservice.competition.model.Competition;
import pl.echelon133.competitionservice.competition.model.Group;
import pl.echelon133.competitionservice.competition.model.Legend;

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
        private List<Group> groups = List.of();
        private List<Legend> legend = List.of();
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

        public CompetitionBuilder groups(List<Group> groups) {
            this.groups = groups;
            return this;
        }

        public CompetitionBuilder legend(List<Legend> legend) {
            this.legend = legend;
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
            var competition = new Competition(name, season, logoUrl, groups, legend);
            competition.setPinned(pinned);
            competition.setId(id);
            competition.setDeleted(deleted);
            return competition;
        }
    }
}
