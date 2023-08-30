package ml.echelon133.matchservice.player.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.player.dto.PlayerDto;
import ml.echelon133.matchservice.player.model.UpsertPlayerDto;
import ml.echelon133.matchservice.player.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlayerService {

    public static final String DATE_OF_BIRTH_FORMAT = "yyyy/MM/d";

    private PlayerRepository playerRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    /**
     * Returns the information about the player with specified id.
     *
     * @param id id of the player
     * @return a dto representing the player
     * @throws ResourceNotFoundException thrown when the player does not exist in the database
     */
    public PlayerDto findById(UUID id) throws ResourceNotFoundException {
        return null;
    }

    /**
     * Updates the player's information.
     *
     * @param id id of the player to update
     * @param playerDto dto containing values to be placed in the database
     * @return a dto representing the updated player
     * @throws ResourceNotFoundException thrown when the player does not exist in the database
     */
    public PlayerDto updatePlayer(UUID id, UpsertPlayerDto playerDto) throws ResourceNotFoundException {
        return null;
    }

    /**
     * Creates the player's entry in the database.
     *
     * @param playerDto dto representing the information about a player that will be saved in the database
     * @return a dto representing the newly saved player
     */
    public PlayerDto createPlayer(UpsertPlayerDto playerDto) {
        return null;
    }

    /**
     * Finds all players whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the player
     * @param pageable information about the wanted page
     * @return a page of players which match the filter
     */
    public Page<PlayerDto> findPlayersByName(String phrase, Pageable pageable) {
        return null;
    }

    /**
     * Marks a player with the specified id as deleted.
     *
     * @param id id of the player to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markPlayerAsDeleted(UUID id)  {
        return null;
    }
}
