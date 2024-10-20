package ml.echelon133.matchservice.team;

import ml.echelon133.matchservice.team.model.UpsertTeamDto;

import java.util.UUID;

public interface TestUpsertTeamDto {
    static UpsertTeamDtoBuilder builder() {
        return new UpsertTeamDtoBuilder();
    }

    class UpsertTeamDtoBuilder {
        private String name = "Test team";
        private String crestUrl = "https://cdn.statically.io/img/test.com/f=auto/image.png";
        private String countryCode = "PL";
        private String coachId = UUID.randomUUID().toString();

        private UpsertTeamDtoBuilder() {}

        public UpsertTeamDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UpsertTeamDtoBuilder crestUrl(String crestUrl) {
            this.crestUrl = crestUrl;
            return this;
        }

        public UpsertTeamDtoBuilder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public UpsertTeamDtoBuilder coachId(String coachId) {
            this.coachId = coachId;
            return this;
        }

        public UpsertTeamDto build() {
            return new UpsertTeamDto(name, crestUrl, countryCode, coachId);
        }
    }
}
