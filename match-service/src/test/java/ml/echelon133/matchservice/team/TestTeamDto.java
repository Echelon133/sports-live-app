package ml.echelon133.matchservice.team;

import ml.echelon133.matchservice.coach.model.CoachDto;
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
        private String countryCode = "PL";
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

        public TeamDtoBuilder countryCode(String countryCode) {
            this.countryCode = countryCode;
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
                    countryCode,
                    coachDto
            );
        }
    }
}
