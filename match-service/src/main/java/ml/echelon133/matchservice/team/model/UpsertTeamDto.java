package ml.echelon133.matchservice.team.model;

import ml.echelon133.common.validator.ValidUUID;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class UpsertTeamDto {

    @NotNull(message = "field has to be provided")
    @Length(min = 1, max = 200, message = "expected length between 1 and 200")
    private String name;

    @NotNull(message = "field has to be provided")
    @ValidUUID
    private String countryId;

    public UpsertTeamDto() {}
    public UpsertTeamDto(String name, String countryId) {
        this.name = name;
        this.countryId = countryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public static UpsertTeamDto.Builder builder() {
        return new UpsertTeamDto.Builder();
    }

    public static class Builder {
        private String name = "Test team";
        private String countryId = UUID.randomUUID().toString();

        private Builder() {}

        public UpsertTeamDto.Builder name(String name) {
            this.name = name;
            return this;
        }

        public UpsertTeamDto.Builder countryId(String countryId) {
            this.countryId = countryId;
            return this;
        }

        public UpsertTeamDto build() {
            return new UpsertTeamDto(name, countryId);
        }
    }
}
