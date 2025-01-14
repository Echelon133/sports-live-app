package pl.echelon133.competitionservice.competition.model;

import java.util.UUID;

public interface CompetitionDto {
    UUID getId();
    String getName();
    String getSeason();
    String getLogoUrl();
    int getMaxRounds();

    static CompetitionDto from(UUID id, String name, String season, String logoUrl, int maxRounds) {
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
            public int getMaxRounds() {
                return maxRounds;
            }
        };
    }

}
