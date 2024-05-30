package ml.echelon133.matchservice.team.model;

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

    @Value("#{T(ml.echelon133.matchservice.team.model.TeamPlayerDto.PlayerShortInfoDto).from(target.playerId, target.name, target.dateOfBirth)}")
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
}
