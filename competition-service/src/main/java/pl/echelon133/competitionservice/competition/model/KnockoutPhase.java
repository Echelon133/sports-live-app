package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.*;
import ml.echelon133.common.entity.BaseEntity;

import java.util.List;

@Entity
public class KnockoutPhase extends BaseEntity {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<Stage> stages;

    public KnockoutPhase() {}
    public KnockoutPhase(List<Stage> stages) {
        this.stages = stages;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages;
    }
}
