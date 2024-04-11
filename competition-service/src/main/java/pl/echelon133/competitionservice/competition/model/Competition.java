package pl.echelon133.competitionservice.competition.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Competition extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 30)
    private String season;

    @Column(name = "logo_url", nullable = false, length = 500)
    private String logoUrl;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group> groups;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Legend> legend;

    public Competition() {}
    public Competition(String name, String season, String logoUrl) {
        this.name = name;
        this.season = season;
        this.logoUrl = logoUrl;
    }
    public Competition(String name, String season, String logoUrl, List<Group> groups, List<Legend> legend) {
        this(name, season, logoUrl);
        this.groups = groups;
        this.legend = legend;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

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
