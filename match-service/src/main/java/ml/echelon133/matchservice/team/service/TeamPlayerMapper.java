package ml.echelon133.matchservice.team.service;

import ml.echelon133.matchservice.team.model.TeamPlayerDto;
import ml.echelon133.matchservice.team.model.TeamPlayer;

public class TeamPlayerMapper {

    private TeamPlayerMapper() {}

    public static TeamPlayerDto entityToDto(TeamPlayer teamPlayer) {
        return TeamPlayerDto.from(
                teamPlayer.getId(),
                TeamPlayerDto.PlayerShortInfoDto.from(
                        teamPlayer.getPlayer().getId(),
                        teamPlayer.getPlayer().getName(),
                        teamPlayer.getPlayer().getDateOfBirth()
                ),
                teamPlayer.getPosition().toString(),
                teamPlayer.getNumber(),
                teamPlayer.getPlayer().getCountryCode()
        );
    }
}
