package ml.echelon133.matchservice.team.model;

import ml.echelon133.common.entity.BaseEntity;
import ml.echelon133.matchservice.country.model.Country;

import javax.persistence.*;

@Entity
public class Team extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    public Team() {}
    public Team(String name, Country country) {
        this.name = name;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }
}
