package ml.echelon133.matchservice.team.model;

import ml.echelon133.common.entity.BaseEntity;
import ml.echelon133.matchservice.coach.model.Coach;

import javax.persistence.*;

@Entity
public class Team extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "crest_url", nullable = false)
    private String crestUrl;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    public Team() {}
    public Team(String name, String crestUrl, String countryCode, Coach coach) {
        this.name = name;
        this.crestUrl = crestUrl;
        this.countryCode = countryCode;
        this.coach = coach;
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

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Coach getCoach() {
        return coach;
    }

    public void setCoach(Coach coach) {
        this.coach = coach;
    }
}
