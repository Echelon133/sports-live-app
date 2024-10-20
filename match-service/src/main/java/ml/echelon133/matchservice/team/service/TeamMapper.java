package ml.echelon133.matchservice.team.service;

import ml.echelon133.matchservice.coach.model.Coach;
import ml.echelon133.matchservice.coach.model.CoachDto;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.TeamDto;

public class TeamMapper {

    private TeamMapper() {}

    public static TeamDto entityToDto(Team team) {
        return TeamDto.from(
                team.getId(),
                team.getName(),
                team.getCrestUrl(),
                team.getCountryCode(),
                CoachDto.from(team.getCoach().getId(), team.getCoach().getName())
        );
    }

    public static Team dtoToEntity(TeamDto dto) {
        Coach coach = null;
        if (dto.getCoach() != null) {
            coach = new Coach(dto.getCoach().getName());
            coach.setId(dto.getCoach().getId());
        }
        var team = new Team(dto.getName(), dto.getCrestUrl(), dto.getCountryCode(), coach);
        team.setId(dto.getId());
        return team;
    }
}
