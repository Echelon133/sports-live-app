package ml.echelon133.matchservice.player.model;

import ml.echelon133.common.constraints.CountryCodeFormat;
import ml.echelon133.common.constraints.LocalDateFormat;
import ml.echelon133.matchservice.player.model.constraints.PositionValue;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotNull;

public class UpsertPlayerDto {

    @NotNull
    @Length(min = 1, max = 200)
    private String name;

    @NotNull
    @CountryCodeFormat
    private String countryCode;

    @NotNull
    @PositionValue
    private String position;

    @NotNull
    @LocalDateFormat
    private String dateOfBirth;

    public UpsertPlayerDto() {}
    public UpsertPlayerDto(String name, String countryCode, String position, String dateOfBirth) {
        this.name = name;
        this.countryCode = countryCode;
        this.position = position;
        this.dateOfBirth = dateOfBirth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @NotNull String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
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
