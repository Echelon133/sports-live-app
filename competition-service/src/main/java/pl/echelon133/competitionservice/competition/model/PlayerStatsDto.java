package pl.echelon133.competitionservice.competition.model;

import java.util.UUID;

public interface PlayerStatsDto {
    UUID getPlayerId();
    UUID getTeamId();
    String getName();
    int getGoals();
    int getAssists();
    int getYellowCards();
    int getRedCards();

    static PlayerStatsDto from(UUID playerId, UUID teamId, String name) {
        return new PlayerStatsDto() {
            @Override
            public UUID getPlayerId() {
                return playerId;
            }

            @Override
            public UUID getTeamId() {
                return teamId;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getGoals() {
                return 0;
            }

            @Override
            public int getAssists() {
                return 0;
            }

            @Override
            public int getYellowCards() {
                return 0;
            }

            @Override
            public int getRedCards() {
                return 0;
            }
        };
    }
}
