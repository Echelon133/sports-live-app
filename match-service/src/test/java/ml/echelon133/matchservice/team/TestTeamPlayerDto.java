package ml.echelon133.matchservice.team;

import ml.echelon133.common.team.dto.TeamPlayerDto;

import java.time.LocalDate;
import java.util.UUID;

public interface TestTeamPlayerDto {
    static TeamPlayerDtoBuilder builder() {
        return new TeamPlayerDtoBuilder();
    }

    class TeamPlayerDtoBuilder {
        private UUID id = UUID.randomUUID();
        private String position = "GOALKEEPER";
        private Integer number  = 1;
        private String countryCode = "PL";
        private UUID playerId = UUID.randomUUID();
        private String playerName = "Test Player";
        private LocalDate playerDateOfBirth = LocalDate.of(1970, 1, 1);

        public TeamPlayerDtoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public TeamPlayerDtoBuilder position(String position) {
            this.position = position;
            return this;
        }

        public TeamPlayerDtoBuilder number(Integer number) {
            this.number = number;
            return this;
        }

        public TeamPlayerDtoBuilder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public TeamPlayerDtoBuilder playerId(UUID playerId) {
            this.playerId = playerId;
            return this;
        }

        public TeamPlayerDtoBuilder playerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        public TeamPlayerDtoBuilder playerDateOfBirth(LocalDate playerDateOfBirth) {
            this.playerDateOfBirth = playerDateOfBirth;
            return this;
        }

        public TeamPlayerDto build() {
            return TeamPlayerDto.from(
                    id,
                    TeamPlayerDto.PlayerShortInfoDto.from(playerId, playerName, playerDateOfBirth),
                    position,
                    number,
                    countryCode
            );
        }
    }
}
