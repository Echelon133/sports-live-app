package ml.echelon133.matchservice.referee.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Referee extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    public Referee() {}
    public Referee(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
