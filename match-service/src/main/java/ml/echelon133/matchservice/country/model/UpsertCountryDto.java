package ml.echelon133.matchservice.country.model;

import ml.echelon133.matchservice.country.model.validator.CountryCodeFormat;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

public class UpsertCountryDto {

    @NotNull(message = "field has to be provided")
    @Length(min = 1, max = 100, message = "expected length between 1 and 100")
    private String name;

    @CountryCodeFormat
    private String countryCode;

    public UpsertCountryDto() {}
    public UpsertCountryDto(String name, String countryCode) {
        this.name = name;
        this.countryCode = countryCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
