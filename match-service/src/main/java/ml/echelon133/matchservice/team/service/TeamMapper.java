package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.country.dto.CountryDto;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.team.model.Team;

public class TeamMapper {

    public static TeamDto entityToDto(Team team) {
        return TeamDto.from(
                team.getId(),
                team.getName(),
                CountryDto.from(team.getCountry().getId(), team.getCountry().getName(), team.getCountry().getCountryCode())
        );
    }

    public static Team dtoToEntity(TeamDto dto) {
        Country country = null;
        if (dto.getCountry() != null) {
            country = new Country(dto.getCountry().getName(), dto.getCountry().getCountryCode());
            country.setId(dto.getCountry().getId());
        }
        var team = new Team(dto.getName(), country);
        team.setId(dto.getId());
        return team;
    }
}
