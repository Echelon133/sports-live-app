package ml.echelon133.common.player.dto;

import ml.echelon133.common.country.dto.CountryDto;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.UUID;

public interface PlayerDto {
    UUID getId();
    String getName();
    String getPosition();
    LocalDate getDateOfBirth();

    // if country is deleted, set this value to null to prevent any leakage of data (seems to be the simplest solution while using native queries)
    @Value("#{target.countryDeleted ? null : (T(ml.echelon133.common.country.dto.CountryDto).from(target.countryId, target.countryName, target.countryCode))}")
    CountryDto getCountry();

    static PlayerDto from(UUID id, String name, String position, LocalDate dateOfBirth, CountryDto countryDto) {
        return new PlayerDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getPosition() {
                return position;
            }

            @Override
            public LocalDate getDateOfBirth() {
                return dateOfBirth;
            }

            @Override
            public CountryDto getCountry() {
                return countryDto;
            }
        };
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private UUID id = UUID.randomUUID();
        private String name = "Test Player";
        private String position = "FORWARD";
        private LocalDate dateOfBirth = LocalDate.of(1970, 1, 1);
        private CountryDto countryDto = CountryDto.from(UUID.randomUUID(), "Test Country", "TC");

        private Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder position(String position) {
            this.position = position;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder countryDto(CountryDto countryDto) {
            this.countryDto = countryDto;
            return this;
        }

        public PlayerDto build() {
            return PlayerDto.from(
                    id,
                    name,
                    position,
                    dateOfBirth,
                    countryDto
            );
        }
    }
}
