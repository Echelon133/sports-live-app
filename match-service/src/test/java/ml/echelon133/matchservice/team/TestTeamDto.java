package ml.echelon133.matchservice.team;

import ml.echelon133.matchservice.coach.model.CoachDto;
import ml.echelon133.matchservice.country.model.CountryDto;
import ml.echelon133.matchservice.team.model.TeamDto;

import java.util.UUID;

public interface TestTeamDto {
    static TeamDtoBuilder builder() {
        return new TeamDtoBuilder();
    }

    class TeamDtoBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Test Team";
        private String crestUrl = "https://cdn.statically.io/img/test.com/f=auto/image.png";
        private CountryDto countryDto = CountryDto.from(UUID.randomUUID(), "Test Country", "TC");
        private CoachDto coachDto = CoachDto.from(UUID.randomUUID(), "Test Coach");

        private TeamDtoBuilder() {}

        public TeamDtoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public TeamDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TeamDtoBuilder crestUrl(String crestUrl) {
            this.crestUrl = crestUrl;
            return this;
        }

        public TeamDtoBuilder countryDto(CountryDto countryDto) {
            this.countryDto = countryDto;
            return this;
        }

        public TeamDtoBuilder coachDto(CoachDto coachDto) {
            this.coachDto = coachDto;
            return this;
        }

        public TeamDto build() {
            return TeamDto.from(
                    id,
                    name,
                    crestUrl,
                    countryDto,
                    coachDto
            );
        }
    }
}
