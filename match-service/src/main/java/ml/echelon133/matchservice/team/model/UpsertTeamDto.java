package ml.echelon133.matchservice.team.model;

import ml.echelon133.common.validator.UUID;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;

public class UpsertTeamDto {

    @NotNull(message = "field has to be provided")
    @Length(min = 1, max = 200, message = "expected length between 1 and 200")
    private String name;

    @NotNull(message = "field has to be provided")
    @URL(message = "not a valid url")
    private String crestUrl;

    @NotNull(message = "field has to be provided")
    @UUID
    private String countryId;

    @NotNull(message = "field has to be provided")
    @UUID
    private String coachId;

    public UpsertTeamDto() {}
    public UpsertTeamDto(String name, String crestUrl, String countryId, String coachId) {
        this.name = name;
        this.crestUrl = crestUrl;
        this.countryId = countryId;
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

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public String getCoachId() {
        return coachId;
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }
}
