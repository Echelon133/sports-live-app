package ml.echelon133.matchservice.player.service;

import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.PlayerDto;
import ml.echelon133.matchservice.player.model.Position;

public class PlayerMapper {

    private PlayerMapper() {}

    public static PlayerDto entityToDto(Player player) {
        return PlayerDto.from(
                player.getId(),
                player.getName(),
                player.getPosition().name(),
                player.getDateOfBirth(),
                player.getCountryCode()
        );
    }

    public static Player dtoToEntity(PlayerDto dto) {
        var player = new Player(
                dto.getName(),
                Position.valueOfIgnoreCase(dto.getPosition()),
                dto.getDateOfBirth(),
                dto.getCountryCode()
        );
        player.setId(dto.getId());
        return player;
    }
}
