package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;

import java.util.List;

@Embeddable
public class LeaguePhase {

    public LeaguePhase() {}
    public LeaguePhase(List<Group> groups, List<Legend> legend) {
        this.groups = groups;
        this.legend = legend;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group> groups;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Legend> legend;

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<Legend> getLegend() {
        return legend;
    }

    public void setLegend(List<Legend> legend) {
        this.legend = legend;
    }
}
