package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import ml.echelon133.common.entity.BaseEntity;

@Entity
public class Competition extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 30)
    private String season;

    @Column(name = "logo_url", nullable = false, length = 500)
    private String logoUrl;

    @Embedded
    private LeaguePhase leaguePhase;

    private boolean pinned;

    public Competition() {}
    public Competition(String name, String season, String logoUrl) {
        this.name = name;
        this.season = season;
        this.logoUrl = logoUrl;
        this.leaguePhase = null;
    }

    public Competition(String name, String season, String logoUrl, LeaguePhase leaguePhase) {
        this(name, season, logoUrl);
        this.leaguePhase = leaguePhase;
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

    public LeaguePhase getLeaguePhase() {
        return leaguePhase;
    }

    public void setLeaguePhase(LeaguePhase leaguePhase) {
        this.leaguePhase = leaguePhase;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
