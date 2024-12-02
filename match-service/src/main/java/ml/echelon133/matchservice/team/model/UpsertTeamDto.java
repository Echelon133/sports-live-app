package ml.echelon133.matchservice.team.model;

import ml.echelon133.common.constraints.CountryCodeFormat;
import ml.echelon133.matchservice.coach.constraints.CoachExists;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotNull;

public class UpsertTeamDto {

    @NotNull
    @Length(min = 1, max = 200)
    private String name;

    @NotNull
    @URL
    private String crestUrl;

    @NotNull
    @CountryCodeFormat
    private String countryCode;

    @NotNull
    @CoachExists
    private String coachId;

    public UpsertTeamDto() {}
    public UpsertTeamDto(String name, String crestUrl, String countryCode, String coachId) {
        this.name = name;
        this.crestUrl = crestUrl;
        this.countryCode = countryCode;
        this.coachId = coachId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCrestUrl() {
        return crestUrl;
    }

    public void setCrestUrl(String crestUrl) {
        this.crestUrl = crestUrl;
    }

    public @NotNull String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(@NotNull String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCoachId() {
        return coachId;
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }
}
