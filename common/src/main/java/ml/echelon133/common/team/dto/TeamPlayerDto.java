package ml.echelon133.common.team.dto;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.UUID;

public interface TeamPlayerDto {
    UUID getId();
    String getPosition();
    Integer getNumber();

    // if country is deleted, set this value to null to prevent any leakage of data (seems to be the simplest solution while using native queries)
    @Value("#{target.countryDeleted ? null : (target.countryCode)}")
    String getCountryCode();

    @Value("#{T(ml.echelon133.common.team.dto.TeamPlayerDto.PlayerShortInfoDto).from(target.playerId, target.name, target.dateOfBirth)}")
    PlayerShortInfoDto getPlayer();

    static TeamPlayerDto from(UUID id, PlayerShortInfoDto playerShortInfoDto, String position, Integer number, String countryCode) {
        return new TeamPlayerDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public PlayerShortInfoDto getPlayer() {
                return playerShortInfoDto;
            }

            @Override
            public String getPosition() {
                return position;
            }

            @Override
            public Integer getNumber() {
                return number;
            }

            @Override
            public String getCountryCode() {
                return countryCode;
            }
        };
    }

    interface PlayerShortInfoDto {
        UUID getId();
        String getName();
        LocalDate getDateOfBirth();

        static PlayerShortInfoDto from(UUID playerId, String name, LocalDate dateOfBirth) {
            return new PlayerShortInfoDto() {
                @Override
                public UUID getId() {
                    return playerId;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public LocalDate getDateOfBirth() {
                    return dateOfBirth;
                }
            };
        }
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private UUID id = UUID.randomUUID();
        private String position = "GOALKEEPER";
        private Integer number  = 1;
        private String countryCode = "PL";
        private UUID playerId = UUID.randomUUID();
        private String playerName = "Test Player";
        private LocalDate playerDateOfBirth = LocalDate.of(1970, 1, 1);

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder position(String position) {
            this.position = position;
            return this;
        }

        public Builder number(Integer number) {
            this.number = number;
            return this;
        }

        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder playerId(UUID playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder playerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        public Builder playerDateOfBirth(LocalDate playerDateOfBirth) {
            this.playerDateOfBirth = playerDateOfBirth;
            return this;
        }

        public TeamPlayerDto build() {
            return TeamPlayerDto.from(
                    id,
                    PlayerShortInfoDto.from(playerId, playerName, playerDateOfBirth),
                    position,
                    number,
                    countryCode
            );
        }
    }
}
