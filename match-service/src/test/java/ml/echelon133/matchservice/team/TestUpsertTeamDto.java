package ml.echelon133.matchservice.team;

import ml.echelon133.matchservice.team.model.UpsertTeamDto;

import java.util.UUID;

public interface TestUpsertTeamDto {
    static UpsertTeamDtoBuilder builder() {
        return new UpsertTeamDtoBuilder();
    }

    class UpsertTeamDtoBuilder {
        private String name = "Test team";
        private String countryId = UUID.randomUUID().toString();
        private String coachId = UUID.randomUUID().toString();

        private UpsertTeamDtoBuilder() {}

        public UpsertTeamDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UpsertTeamDtoBuilder countryId(String countryId) {
            this.countryId = countryId;
            return this;
        }

        public UpsertTeamDtoBuilder coachId(String coachId) {
            this.coachId = coachId;
            return this;
        }

        public UpsertTeamDto build() {
            return new UpsertTeamDto(name, countryId, coachId);
        }
    }
}
