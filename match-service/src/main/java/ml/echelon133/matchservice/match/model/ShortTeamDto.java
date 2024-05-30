package ml.echelon133.matchservice.match.model;

import java.util.UUID;

public interface ShortTeamDto {
    UUID getId();
    String getName();
    String getCrestUrl();

    static ShortTeamDto from(UUID teamId, String teamName, String teamCrestUrl) {
        return new ShortTeamDto() {
            @Override
            public UUID getId() {
                return teamId;
            }

            @Override
            public String getName() {
                return teamName;
            }

            @Override
            public String getCrestUrl() {
                return teamCrestUrl;
            }
        };
    }
}
