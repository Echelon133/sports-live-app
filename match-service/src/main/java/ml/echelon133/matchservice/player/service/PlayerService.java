package ml.echelon133.matchservice.player.service;

import ml.echelon133.common.constants.DateFormatConstants;
import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.player.dto.PlayerDto;
import ml.echelon133.matchservice.country.service.CountryService;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.player.model.UpsertPlayerDto;
import ml.echelon133.matchservice.player.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class PlayerService {

    public static final String DATE_OF_BIRTH_FORMAT = DateFormatConstants.DATE_FORMAT;
    public static final DateTimeFormatter DATE_OF_BIRTH_FORMATTER = DateTimeFormatter.ofPattern(DATE_OF_BIRTH_FORMAT);

    private final PlayerRepository playerRepository;
    private final CountryService countryService;

    @Autowired
    public PlayerService(PlayerRepository playerRepository, CountryService countryService) {
        this.playerRepository = playerRepository;
        this.countryService = countryService;
    }

    /**
     * Returns the information about the player with specified id.
     *
     * @param id id of the player
     * @return a dto representing the player
     * @throws ResourceNotFoundException thrown when the player does not exist in the database
     */
    public PlayerDto findById(UUID id) throws ResourceNotFoundException {
        return playerRepository
                .findPlayerById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Player.class, id));
    }

    /**
     * Updates the player's information.
     *
     * The values in {@link UpsertPlayerDto} have to be pre-validated before being used here, otherwise
     * incorrect data will be placed into the database.
     *
     * @param id id of the player to update
     * @param playerDto dto containing updated information about the player
     * @return a dto representing the updated player
     * @throws ResourceNotFoundException thrown when the player or their country does not exist in the database
     */
    public PlayerDto updatePlayer(UUID id, UpsertPlayerDto playerDto) throws ResourceNotFoundException {
        var playerToUpdate = playerRepository
                .findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Player.class, id));

        playerToUpdate.setName(playerDto.getName());

        // this `Position.valueOfIgnoreCase` should never fail because the Position value is pre-validated
        playerToUpdate.setPosition(Position.valueOfIgnoreCase(playerDto.getPosition()));

        // this `LocalDate.parse` should never fail because the DateOfBirth value is pre-validated
        playerToUpdate.setDateOfBirth(LocalDate.parse(playerDto.getDateOfBirth(), DATE_OF_BIRTH_FORMATTER));

        // this `UUID.fromString` should never fail because the CountryId value is pre-validated
        var countryId = UUID.fromString(playerDto.getCountryId());
        var country = countryService.findEntityById(countryId);
        playerToUpdate.setCountry(country);

        return PlayerMapper.entityToDto(playerRepository.save(playerToUpdate));
    }

    /**
     * Creates the player's entry in the database.
     *
     * The values in {@link UpsertPlayerDto} have to be pre-validated before being used here, otherwise
     * incorrect data will be placed into the database.
     *
     * @param playerDto dto representing the information about a player that will be saved in the database
     * @return a dto representing the newly saved player
     * @throws ResourceNotFoundException thrown when the player's country does not exist in the database
     */
    public PlayerDto createPlayer(UpsertPlayerDto playerDto) throws ResourceNotFoundException {
        // this `Position.valueOfIgnoreCase` should never fail because the Position value is pre-validated
        var position = Position.valueOfIgnoreCase(playerDto.getPosition());

        // this `LocalDate.parse` should never fail because the DateOfBirth value is pre-validated
        var dateOfBirth = LocalDate.parse(playerDto.getDateOfBirth(), DATE_OF_BIRTH_FORMATTER);

        // this `UUID.fromString` should never fail because the CountryId value is pre-validated
        var countryId = UUID.fromString(playerDto.getCountryId());
        var country = countryService.findEntityById(countryId);

        var player = new Player(playerDto.getName(), position, dateOfBirth, country);
        return PlayerMapper.entityToDto(playerRepository.save(player));
    }

    /**
     * Finds all players whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the player
     * @param pageable information about the wanted page
     * @return a page of players which match the filter
     */
    public Page<PlayerDto> findPlayersByName(String phrase, Pageable pageable) {
        return playerRepository.findAllByNameContaining(phrase, pageable);
    }

    /**
     * Marks a player with the specified id as deleted.
     *
     * @param id id of the player to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markPlayerAsDeleted(UUID id)  {
        return playerRepository.markPlayerAsDeleted(id);
    }
}
