package ml.echelon133.matchservice.player;

import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.player.model.UpsertPlayerDto;

import java.util.UUID;

public interface TestUpsertPlayerDto {

    static UpsertPlayerDtoBuilder builder() {
        return new UpsertPlayerDtoBuilder();
    }

    class UpsertPlayerDtoBuilder {
        private String name = "Test player";
        private String countryId = UUID.randomUUID().toString();
        private String position = Position.FORWARD.name();
        private String dateOfBirth = "1970/01/01";

        private UpsertPlayerDtoBuilder() {}

        public UpsertPlayerDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UpsertPlayerDtoBuilder countryId(String countryId) {
            this.countryId = countryId;
            return this;
        }

        public UpsertPlayerDtoBuilder position(String position) {
            this.position = position;
            return this;
        }

        public UpsertPlayerDtoBuilder dateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public UpsertPlayerDto build() {
            return new UpsertPlayerDto(name, countryId, position, dateOfBirth);
        }
    }
}
