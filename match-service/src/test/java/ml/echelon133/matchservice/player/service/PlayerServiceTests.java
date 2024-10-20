package ml.echelon133.matchservice.player.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.player.TestPlayerDto;
import ml.echelon133.matchservice.player.TestUpsertPlayerDto;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.PlayerDto;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.player.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PlayerServiceTests {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;


    @Test
    @DisplayName("findById throws when the repository does not store an entity with the given id")
    public void findById_EntityNotPresent_Throws() {
        var playerId = UUID.randomUUID();

        // given
        given(playerRepository.findPlayerById(playerId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            playerService.findById(playerId);
        }).getMessage();

        // then
        assertEquals(String.format("player %s could not be found", playerId), message);
    }

    @Test
    @DisplayName("findById returns the dto when the player is present")
    public void findById_EntityPresent_ReturnsDto() throws ResourceNotFoundException {
        var testDto = TestPlayerDto.builder().build();
        var playerId = testDto.getId();

        // given
        given(playerRepository.findPlayerById(playerId)).willReturn(Optional.of(testDto));

        // when
        PlayerDto dto = playerService.findById(playerId);

        // then
        assertEquals(testDto, dto);
    }

    @Test
    @DisplayName("findEntityById throws when the repository does not store an entity with given id")
    public void findEntityById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(playerRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            playerService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("player %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById throws when the repository stores an entity with given id but it's deleted")
    public void findEntityById_EntityPresentButDeleted_Throws() {
        var testId = UUID.randomUUID();
        var playerEntity = new Player();
        playerEntity.setDeleted(true);

        // given
        given(playerRepository.findById(testId)).willReturn(Optional.of(playerEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            playerService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("player %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById returns the entity when the repository stores it")
    public void findEntityById_EntityPresent_ReturnsEntity() throws ResourceNotFoundException {
        var testId = UUID.randomUUID();
        var playerEntity = new Player();

        // given
        given(playerRepository.findById(testId)).willReturn(Optional.of(playerEntity));

        // when
        var entity = playerService.findEntityById(testId);

        // then
        assertEquals(playerEntity, entity);
    }

    @Test
    @DisplayName("updatePlayer throws when the player to update does not exist")
    public void updatePlayer_PlayerToUpdateEmpty_Throws() {
        var playerId = UUID.randomUUID();

        // given
        given(playerRepository.findById(playerId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            playerService.updatePlayer(playerId, TestUpsertPlayerDto.builder().build());
        }).getMessage();

        // then
        assertEquals(String.format("player %s could not be found", playerId), message);
    }

    @Test
    @DisplayName("updatePlayer throws when the player to update is marked as deleted")
    public void updatePlayer_PlayerToUpdatePresentButMarkedAsDeleted_Throws() {
        var entity = new Player();
        entity.setDeleted(true);
        var playerId = entity.getId();

        // given
        given(playerRepository.findById(playerId)).willReturn(Optional.of(entity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            playerService.updatePlayer(playerId, TestUpsertPlayerDto.builder().build());
        }).getMessage();

        // then
        assertEquals(String.format("player %s could not be found", playerId), message);
    }

    @Test
    @DisplayName("updatePlayer returns the expected dto after correctly updating a player")
    public void updatePlayer_PlayerUpdated_ReturnsDto() throws ResourceNotFoundException {
        var oldPlayer = new Player("Test", Position.DEFENDER, LocalDate.of(1970, 1, 1), "PL");
        var newCountry = "DE";
        var updateDto = TestUpsertPlayerDto.builder()
                .name("Some name")
                .dateOfBirth("1980/01/01")
                .position("FORWARD")
                .countryCode(newCountry)
                .build();
        var newDateOfBirth = LocalDate.parse(updateDto.getDateOfBirth(), PlayerService.DATE_OF_BIRTH_FORMATTER);
        var expectedPlayer = new Player(
                updateDto.getName(),
                Position.valueOfIgnoreCase(updateDto.getPosition()),
                newDateOfBirth,
                newCountry
        );
        expectedPlayer.setId(oldPlayer.getId());

        // given
        given(playerRepository.findById(oldPlayer.getId())).willReturn(Optional.of(oldPlayer));
        given(playerRepository.save(argThat(p ->
                // Regular eq() only compares by entity's ID, which means that we need to use argThat()
                // if we want to make sure that the code actually tries to save a player with updated
                // values. Using eq() would make this test pass even if the method tried to save the
                // player without making any changes to it.
                p.getId().equals(oldPlayer.getId()) &&
                        p.getName().equals(updateDto.getName()) &&
                        p.getPosition().name().equals(updateDto.getPosition()) &&
                        p.getCountryCode().equals(updateDto.getCountryCode()) &&
                        p.getDateOfBirth().equals(newDateOfBirth)
                ))).willReturn(expectedPlayer);

        // when
        var playerDto = playerService.updatePlayer(oldPlayer.getId(), updateDto);

        // then
        assertEquals(oldPlayer.getId(), playerDto.getId());
        assertEquals(updateDto.getName(), playerDto.getName());
        assertEquals(updateDto.getPosition(), playerDto.getPosition());
        assertEquals(newDateOfBirth, playerDto.getDateOfBirth());
        assertEquals(updateDto.getCountryCode(), playerDto.getCountryCode());
    }

    @Test
    @DisplayName("markPlayerAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markPlayerAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(playerRepository.markPlayerAsDeleted(idToDelete)).willReturn(1);

        // when
        Integer countDeleted = playerService.markPlayerAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("findPlayersByName correctly calls the repository method")
    public void findPlayersByName_CustomPhraseAndPageable_CorrectlyCallsRepository() {
        var phrase = "test";
        var pageable = Pageable.ofSize(7).withPage(4);
        var expectedDto = TestPlayerDto.builder().name(phrase).build();
        var expectedPage = new PageImpl<>(List.of(expectedDto), pageable, 1);

        // given
        given(playerRepository.findAllByNameContaining(
                eq(phrase),
                argThat(p -> p.getPageSize() == 7 && p.getPageNumber() == 4)
        )).willReturn(expectedPage);

        // when
        var result = playerService.findPlayersByName(phrase, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
    }

    @Test
    @DisplayName("createPlayer returns the expected dto after correctly creating a player")
    public void createPlayer_PlayerCreated_ReturnsDto() throws ResourceNotFoundException {
        var country = "PT";
        var createDto = TestUpsertPlayerDto.builder()
                .name("Some name")
                .dateOfBirth("1980/01/01")
                .position("FORWARD")
                .countryCode(country)
                .build();
        var dateOfBirth = LocalDate.parse(createDto.getDateOfBirth(), PlayerService.DATE_OF_BIRTH_FORMATTER);
        var expectedPlayer = new Player(
                createDto.getName(),
                Position.valueOfIgnoreCase(createDto.getPosition()),
                dateOfBirth,
                country
        );

        // given
        given(playerRepository.save(argThat(p ->
                // Regular eq() only compares by entity's ID, which means that we need to use argThat()
                // if we want to make sure that the code actually tries to save a player whose values
                // are taken from received upsert DTO
                p.getName().equals(createDto.getName()) &&
                p.getPosition().name().equals(createDto.getPosition()) &&
                p.getCountryCode().equals(createDto.getCountryCode()) &&
                p.getDateOfBirth().equals(dateOfBirth)
        ))).willReturn(expectedPlayer);

        // when
        var playerDto = playerService.createPlayer(createDto);

        // then
        assertEquals(expectedPlayer.getId(), playerDto.getId());
        assertEquals(expectedPlayer.getName(), playerDto.getName());
        assertEquals(expectedPlayer.getPosition().name(), playerDto.getPosition());
        assertEquals(expectedPlayer.getCountryCode(), playerDto.getCountryCode());
        assertEquals(expectedPlayer.getDateOfBirth(), playerDto.getDateOfBirth());
    }
}
