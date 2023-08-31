package ml.echelon133.matchservice.player.model;

import ml.echelon133.common.entity.BaseEntity;
import ml.echelon133.matchservice.country.model.Country;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
public class Player extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    private Position position;

    private LocalDate dateOfBirth;

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    public Player() {}
    public Player(String name, Position position, LocalDate dateOfBirth, Country country) {
        this.name = name;
        this.position = position;
        this.dateOfBirth = dateOfBirth;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }
}
