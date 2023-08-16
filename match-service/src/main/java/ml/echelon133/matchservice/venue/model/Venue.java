package ml.echelon133.matchservice.venue.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Venue extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    private Integer capacity;

    public Venue() {}
    public Venue(String name, Integer capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
