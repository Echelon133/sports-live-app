package pl.echelon133.competitionservice.competition.model;

import java.util.UUID;

public interface CompetitionDto {
    UUID getId();
    String getName();
    String getSeason();
    String getLogoUrl();
    boolean getLeaguePhase();
    int getMaxRounds();
    boolean getKnockoutPhase();

    static CompetitionDto from(UUID id, String name, String season, String logoUrl, boolean leaguePhase, int maxRounds, boolean knockoutPhase) {
        return new CompetitionDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getSeason() {
                return season;
            }

            @Override
            public String getLogoUrl() {
                return logoUrl;
            }

            @Override
            public boolean getLeaguePhase() {
                return leaguePhase;
            }

            @Override
            public int getMaxRounds() {
                return maxRounds;
            }

            @Override
            public boolean getKnockoutPhase() {
                return knockoutPhase;
            }
        };
    }
}
