package ml.echelon133.matchservice.player.model;

import ml.echelon133.common.validator.ValidUUID;
import ml.echelon133.matchservice.player.model.validator.ValidLocalDateFormat;
import ml.echelon133.matchservice.player.model.validator.ValidPositionValue;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class UpsertPlayerDto {

    @NotNull(message = "field has to be provided")
    @Length(min = 1, max = 200, message = "expected length between 1 and 200")
    private String name;

    @NotNull(message = "field has to be provided")
    @ValidUUID
    private String countryId;

    @NotNull(message = "field has to be provided")
    @ValidPositionValue
    private String position;

    @NotNull(message = "field has to be provided")
    @ValidLocalDateFormat
    private String dateOfBirth;

    public UpsertPlayerDto() {}
    public UpsertPlayerDto(String name, String countryId, String position, String dateOfBirth) {
        this.name = name;
        this.countryId = countryId;
        this.position = position;
        this.dateOfBirth = dateOfBirth;
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

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "Test player";
        private String countryId = UUID.randomUUID().toString();
        private String position = Position.FORWARD.name();
        private String dateOfBirth = "1970/01/01";

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder countryId(String countryId) {
            this.countryId = countryId;
            return this;
        }

        public Builder position(String position) {
            this.position = position;
            return this;
        }

        public Builder dateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public UpsertPlayerDto build() {
            return new UpsertPlayerDto(name, countryId, position, dateOfBirth);
        }
    }
}
