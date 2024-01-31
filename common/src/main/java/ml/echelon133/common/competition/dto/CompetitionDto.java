package ml.echelon133.common.competition.dto;

import java.util.UUID;

public interface CompetitionDto {
    UUID getId();
    String getName();
    String getSeason();
    String getLogoUrl();

    static CompetitionDto from(UUID id, String name, String season, String logoUrl) {
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
        };
    }

}
