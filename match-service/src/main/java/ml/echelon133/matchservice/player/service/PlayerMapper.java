package ml.echelon133.matchservice.player.service;

import ml.echelon133.matchservice.country.model.CountryDto;
import ml.echelon133.matchservice.player.model.PlayerDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;

public class PlayerMapper {

    public static PlayerDto entityToDto(Player player) {
        return PlayerDto.from(
                player.getId(),
                player.getName(),
                player.getPosition().name(),
                player.getDateOfBirth(),
                CountryDto.from(player.getCountry().getId(), player.getCountry().getName(), player.getCountry().getCountryCode())
        );
    }

    public static Player dtoToEntity(PlayerDto dto) {
        Country country = null;
        if (dto.getCountry() != null) {
            country = new Country(dto.getCountry().getName(), dto.getCountry().getCountryCode());
            country.setId(dto.getCountry().getId());
        }
        var player = new Player(dto.getName(), Position.valueOfIgnoreCase(dto.getPosition()), dto.getDateOfBirth(), country);
        player.setId(dto.getId());
        return player;
    }
}
