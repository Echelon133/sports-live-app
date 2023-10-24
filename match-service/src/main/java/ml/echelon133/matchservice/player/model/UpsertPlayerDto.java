package ml.echelon133.matchservice.player.model;

import ml.echelon133.common.constraints.UUID;
import ml.echelon133.common.constraints.LocalDateFormat;
import ml.echelon133.matchservice.player.model.constraints.PositionValue;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

public class UpsertPlayerDto {

    @NotNull
    @Length(min = 1, max = 200)
    private String name;

    @NotNull
    @UUID
    private String countryId;

    @NotNull
    @PositionValue
    private String position;

    @NotNull
    @LocalDateFormat
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
}
