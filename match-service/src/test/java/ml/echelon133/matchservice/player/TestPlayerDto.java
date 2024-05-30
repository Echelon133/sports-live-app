package ml.echelon133.matchservice.player;

import ml.echelon133.matchservice.country.model.CountryDto;
import ml.echelon133.matchservice.player.model.PlayerDto;

import java.time.LocalDate;
import java.util.UUID;

public interface TestPlayerDto {
    static PlayerDtoBuilder builder() {
        return new PlayerDtoBuilder();
    }

    class PlayerDtoBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Test Player";
        private String position = "FORWARD";
        private LocalDate dateOfBirth = LocalDate.of(1970, 1, 1);
        private CountryDto countryDto = CountryDto.from(UUID.randomUUID(), "Test Country", "TC");

        private PlayerDtoBuilder() {}

        public PlayerDtoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PlayerDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PlayerDtoBuilder position(String position) {
            this.position = position;
            return this;
        }

        public PlayerDtoBuilder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public PlayerDtoBuilder countryDto(CountryDto countryDto) {
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
