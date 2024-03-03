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
}
