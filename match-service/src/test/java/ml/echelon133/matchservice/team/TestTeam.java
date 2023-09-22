package ml.echelon133.matchservice.team;

import ml.echelon133.matchservice.coach.model.Coach;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.team.model.Team;

import java.util.UUID;

public interface TestTeam {
    static TeamBuilder builder() {
        return new TeamBuilder();
    }

    class TeamBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Test Team";
        private String crestUrl = "https://cdn.statically.io/img/test.com/f=auto/image.png";
        private Country country = new Country("Test Country", "TC");
        private Coach coach = new Coach("Test Coach");

        private TeamBuilder() {}

        public TeamBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public TeamBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TeamBuilder crestUrl(String crestUrl) {
            this.crestUrl = crestUrl;
            return this;
        }

        public TeamBuilder country(Country country) {
            this.country = country;
            return this;
        }

        public TeamBuilder coach(Coach coach) {
            this.coach = coach;
            return this;
        }

        public Team build() {
            var team = new Team(name, crestUrl, country, coach);
            team.setId(id);
            return team;
        }
    }
}
