package ml.echelon133.matchservice.coach.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Coach extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    public Coach() {}
    public Coach(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
