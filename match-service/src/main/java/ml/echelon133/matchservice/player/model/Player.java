package ml.echelon133.matchservice.player.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDate;

@Entity
public class Player extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    private Position position;

    private LocalDate dateOfBirth;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    public Player() {}
    public Player(String name, Position position, LocalDate dateOfBirth, String countryCode) {
        this.name = name;
        this.position = position;
        this.dateOfBirth = dateOfBirth;
        this.countryCode = countryCode;
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

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
