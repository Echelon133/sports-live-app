package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;

import java.util.List;

@Embeddable
public class KnockoutPhase {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
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
