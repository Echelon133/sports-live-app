package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.*;
import ml.echelon133.common.entity.BaseEntity;

@Entity
public class Competition extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 30)
    private String season;

    @Column(name = "logo_url", nullable = false, length = 500)
    private String logoUrl;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "league_phase_id")
    private LeaguePhase leaguePhase;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "knockout_phase_id")
    private KnockoutPhase knockoutPhase;

    private boolean pinned;

    public Competition() {}
    public Competition(String name, String season, String logoUrl) {
        this.name = name;
        this.season = season;
        this.logoUrl = logoUrl;
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

    public KnockoutPhase getKnockoutPhase() {
        return knockoutPhase;
    }

    public void setKnockoutPhase(KnockoutPhase knockoutPhase) {
        this.knockoutPhase = knockoutPhase;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
