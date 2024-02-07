package pl.echelon133.competitionservice.competition.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "comp_group")
public class Group extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamStats> teams;

    public Group() {}
    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TeamStats> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamStats> teams) {
        this.teams = teams;
    }
}
