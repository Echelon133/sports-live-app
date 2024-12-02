package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.player.service.PlayerService;
import ml.echelon133.matchservice.team.TestTeam;
import ml.echelon133.matchservice.team.TestTeamDto;
import ml.echelon133.matchservice.team.TestTeamPlayerDto;
import ml.echelon133.matchservice.team.exception.NumberAlreadyTakenException;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import ml.echelon133.matchservice.team.model.UpsertTeamPlayerDto;
import ml.echelon133.matchservice.team.repository.TeamPlayerRepository;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class TeamPlayerServiceTests {

    @Mock
    private TeamPlayerRepository teamPlayerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private TeamPlayerService teamPlayerService;

    private TeamPlayer getTestTeamPlayer() {
        var team = TestTeam.builder().build();
        var player = new Player(
                "Test player",
                Position.GOALKEEPER,
                LocalDate.of(1970, 1, 1),
                "PL"
        );
        return new TeamPlayer(team, player, Position.GOALKEEPER, 1);
    }

    @Test
    @DisplayName("findEntityById throws when the repository does not store an entity with given id")
    public void findEntityById_EntityNotPresent_Throws() {
        var testId = UUID.randomUUID();

        // given
        given(teamPlayerRepository.findById(testId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("teamplayer %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById throws when the repository stores an entity with given id but it's deleted")
    public void findEntityById_EntityPresentButDeleted_Throws() {
        var testId = UUID.randomUUID();
        var teamPlayerEntity = new TeamPlayer();
        teamPlayerEntity.setDeleted(true);

        // given
        given(teamPlayerRepository.findById(testId)).willReturn(Optional.of(teamPlayerEntity));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.findEntityById(testId);
        }).getMessage();

        // then
        assertEquals(String.format("teamplayer %s could not be found", testId), message);
    }

    @Test
    @DisplayName("findEntityById returns the entity when the repository stores it")
    public void findEntityById_EntityPresent_ReturnsEntity() throws ResourceNotFoundException {
        var testId = UUID.randomUUID();
        var teamPlayerEntity = new TeamPlayer();

        // given
        given(teamPlayerRepository.findById(testId)).willReturn(Optional.of(teamPlayerEntity));

        // when
        var entity = teamPlayerService.findEntityById(testId);

        // then
        assertEquals(teamPlayerEntity, entity);
    }

    @Test
    @DisplayName("findAllPlayersOfTeam returns dtos when the team is present")
    public void findAllPlayersOfTeam_TeamPresent_ReturnsDtos() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var teamPlayer = TestTeamPlayerDto.builder().build();

        // given
        given(teamPlayerRepository.findAllPlayersByTeamId(teamId)).willReturn(List.of(teamPlayer));

        // when
        var players = teamPlayerService.findAllPlayersOfTeam(teamId);

        // then
        assertEquals(1, players.size());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayer returns dtos when the player is present")
    public void findAllTeamsOfPlayer_PlayerPresent_ReturnsDtos() throws ResourceNotFoundException {
        var playerId = UUID.randomUUID();
        var team = TestTeamDto.builder().build();

        // given
        given(teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(playerId)).willReturn(List.of(team));

        // when
        var teams = teamPlayerService.findAllTeamsOfPlayer(playerId);

        // then
        assertEquals(1, teams.size());
    }

    @Test
    @DisplayName("createTeamPlayer throws when the player's number is already taken")
    public void createTeamPlayer_NumberTaken_Throws() {
        var teamId = UUID.randomUUID();
        var number = 1;
        var createDto = new UpsertTeamPlayerDto("", "", number);

        // given
        given(teamPlayerRepository.teamHasPlayerWithNumber(teamId, number)).willReturn(true);

        // when
        String message = assertThrows(NumberAlreadyTakenException.class, () -> {
            teamPlayerService.createTeamPlayer(teamId, createDto);
        }).getMessage();

        // then
        assertEquals(
                String.format("team %s already has a player with number %d", teamId, number),
                message
        );
    }

    @Test
    @DisplayName("createTeamPlayer throws when the team is not found")
    public void createTeamPlayer_TeamNotFound_Throws() {
        var teamId = UUID.randomUUID();
        var createDto = new UpsertTeamPlayerDto(null, null, null);

        // given
        given(teamPlayerRepository.teamHasPlayerWithNumber(any(), any())).willReturn(false);
        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.createTeamPlayer(teamId, createDto);
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", teamId), message);
    }

    @Test
    @DisplayName("createTeamPlayer throws when the team is marked as deleted")
    public void createTeamPlayer_TeamMarkedAsDeleted_Throws() {
        var teamId = UUID.randomUUID();
        var createDto = new UpsertTeamPlayerDto(null, null, null);
        var team = TestTeam.builder().build();
        team.setDeleted(true);

        // given
        given(teamPlayerRepository.teamHasPlayerWithNumber(any(), any())).willReturn(false);
        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.createTeamPlayer(teamId, createDto);
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", teamId), message);
    }

    @Test
    @DisplayName("createTeamPlayer throws when the player is not found")
    public void createTeamPlayer_PlayerNotFound_Throws() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var team = TestTeam.builder().build();
        var createDto = new UpsertTeamPlayerDto(playerId.toString(), null, null);

        // given
        given(teamPlayerRepository.teamHasPlayerWithNumber(any(), any())).willReturn(false);
        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(playerService.findEntityById(playerId)).willThrow(
                new ResourceNotFoundException(Player.class, playerId)
        );

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.createTeamPlayer(teamId, createDto);
        }).getMessage();

        // then
        assertEquals(String.format("player %s could not be found", playerId), message);
    }

    @Test
    @DisplayName("createTeamPlayer returns the expected dto after correctly saving the TeamPlayer")
    public void createTeamPlayer_TeamPlayerCreated_ReturnsDto() throws NumberAlreadyTakenException, ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var createDto = new UpsertTeamPlayerDto(playerId.toString(), "GOALKEEPER", 1);
        var entity = getTestTeamPlayer();
        var expectedDto = TestTeamPlayerDto
                .builder()
                .id(entity.getId())
                .position(createDto.position())
                .number(createDto.number())
                .countryCode(entity.getPlayer().getCountryCode())
                .playerName(entity.getPlayer().getName())
                .playerDateOfBirth(entity.getPlayer().getDateOfBirth())
                .playerId(entity.getPlayer().getId())
                .build();

        // given
        given(teamPlayerRepository.teamHasPlayerWithNumber(teamId, createDto.number())).willReturn(false);
        given(teamRepository.findById(teamId)).willReturn(Optional.of(entity.getTeam()));
        given(playerService.findEntityById(playerId)).willReturn(entity.getPlayer());
        given(teamPlayerRepository.save(argThat(tp ->
                tp.getTeam().equals(entity.getTeam()) &&
                    tp.getPlayer().equals(entity.getPlayer()) &&
                    tp.getNumber().equals(createDto.number()) &&
                    tp.getPosition().toString().equals(createDto.position())
        ))).willReturn(entity);

        // when
        var result = teamPlayerService.createTeamPlayer(teamId, createDto);

        // then
        assertEquals(expectedDto.getId(), result.getId());
        assertEquals(expectedDto.getNumber(), result.getNumber());
        assertEquals(expectedDto.getPosition(), result.getPosition());
        assertEquals(expectedDto.getCountryCode(), result.getCountryCode());
        assertEquals(expectedDto.getPlayer().getId(), result.getPlayer().getId());
        assertEquals(expectedDto.getPlayer().getName(), result.getPlayer().getName());
        assertEquals(expectedDto.getPlayer().getDateOfBirth(), result.getPlayer().getDateOfBirth());
    }

    @Test
    @DisplayName("updateTeamPlayer throws when the teamPlayer is not found")
    public void updateTeamPlayer_TeamPlayerNotFound_Throws() {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var updateDto = new UpsertTeamPlayerDto(null, null, null);

        // given
        given(teamPlayerRepository.findById(teamPlayerId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.updateTeamPlayer(teamPlayerId, teamId, updateDto);
        }).getMessage();

        // then
        assertEquals(
                String.format("teamplayer %s could not be found", teamPlayerId),
                message
        );
    }

    @Test
    @DisplayName("updateTeamPlayer throws when the teamPlayer is marked as deleted")
    public void updateTeamPlayer_TeamPlayerMarkedAsDeleted_Throws() {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var updateDto = new UpsertTeamPlayerDto(null, null, null);
        var teamPlayer = new TeamPlayer();
        teamPlayer.setDeleted(true);

        // given
        given(teamPlayerRepository.findById(teamPlayerId)).willReturn(Optional.of(teamPlayer));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.updateTeamPlayer(teamPlayerId, teamId, updateDto);
        }).getMessage();

        // then
        assertEquals(
                String.format("teamplayer %s could not be found", teamPlayerId),
                message
        );
    }

    @Test
    @DisplayName("updateTeamPlayer throws when the team is marked as deleted")
    public void updateTeamPlayer_TeamMarkedAsDeleted_Throws() {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var updateDto = new UpsertTeamPlayerDto(null, null, null);
        var teamPlayer = getTestTeamPlayer();
        teamPlayer.setId(teamPlayerId);
        teamPlayer.getTeam().setDeleted(true);

        // given
        given(teamPlayerRepository.findById(teamPlayerId)).willReturn(Optional.of(teamPlayer));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.updateTeamPlayer(teamPlayerId, teamId, updateDto);
        }).getMessage();

        // then
        assertEquals(String.format("team %s could not be found", teamId), message);
    }

    @Test
    @DisplayName("updateTeamPlayer throws when team id's point to different teams")
    public void updateTeamPlayer_TeamIdAndTeamPlayerTeamIdAreInconsistent_Throws() {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var updateDto = new UpsertTeamPlayerDto(null, null, null);
        var teamPlayer = new TeamPlayer();
        // make sure that the teamId and teamPlayer's Team's teamId are different
        teamPlayer.setTeam(TestTeam.builder().build());

        // given
        given(teamPlayerRepository.findById(teamPlayerId)).willReturn(Optional.of(teamPlayer));

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.updateTeamPlayer(teamPlayerId, teamId, updateDto);
        }).getMessage();

        // then
        assertEquals(
                String.format("teamplayer %s could not be found", teamPlayerId),
                message
        );
    }

    @Test
    @DisplayName("updateTeamPlayer throws when the player's number is already taken")
    public void updateTeamPlayer_NumberTaken_Throws() {
        var teamId = UUID.randomUUID();
        var teamPlayerId = UUID.randomUUID();
        var number = 1;
        var teamPlayer = getTestTeamPlayer();
        teamPlayer.getTeam().setId(teamId);
        var updateDto = new UpsertTeamPlayerDto("", "", number);

        // given
        given(teamPlayerRepository.findById(teamPlayerId)).willReturn(Optional.of(teamPlayer));
        given(teamPlayerRepository.teamHasPlayerWithNumber(teamId, number)).willReturn(true);

        // when
        String message = assertThrows(NumberAlreadyTakenException.class, () -> {
            teamPlayerService.updateTeamPlayer(teamPlayerId, teamId, updateDto);
        }).getMessage();

        // then
        assertEquals(
                String.format("team %s already has a player with number %d", teamId, number),
                message
        );
    }

    @Test
    @DisplayName("updateTeamPlayer throws when the player is not found")
    public void updateTeamPlayer_PlayerNotFound_Throws() throws ResourceNotFoundException {
        var teamId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var teamPlayer = getTestTeamPlayer();
        teamPlayer.getTeam().setId(teamId);
        var updateDto = new UpsertTeamPlayerDto(playerId.toString(), null, null);

        // given
        given(teamPlayerRepository.teamHasPlayerWithNumber(any(), any())).willReturn(false);
        given(teamRepository.findById(teamId)).willReturn(Optional.of(teamPlayer.getTeam()));
        given(playerService.findEntityById(playerId)).willThrow(
                new ResourceNotFoundException(Player.class, playerId)
        );

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            teamPlayerService.createTeamPlayer(teamId, updateDto);
        }).getMessage();

        // then
        assertEquals(String.format("player %s could not be found", playerId), message);
    }

    @Test
    @DisplayName("updateTeamPlayer returns the expected dto after correctly updating the TeamPlayer")
    public void updateTeamPlayer_TeamPlayerUpdated_ReturnsDto() throws NumberAlreadyTakenException, ResourceNotFoundException {
        var oldTeamPlayer = getTestTeamPlayer();
        var teamPlayerId = oldTeamPlayer.getId();
        var teamId = oldTeamPlayer.getTeam().getId();

        var newPlayer = new Player(
                "Player",
                Position.MIDFIELDER,
                LocalDate.of(1990, 1, 1),
                "DE"
        );
        var newPlayerId = newPlayer.getId();
        var newPosition = "MIDFIELDER";
        var newNumber = 50;

        var createDto = new UpsertTeamPlayerDto(newPlayer.getId().toString(), newPosition, newNumber);
        var expectedEntity = new TeamPlayer(
                oldTeamPlayer.getTeam(), newPlayer, Position.valueOfIgnoreCase(newPosition), newNumber
        );
        expectedEntity.setId(oldTeamPlayer.getId());

        var expectedDto = TestTeamPlayerDto
                .builder()
                .id(oldTeamPlayer.getId())
                .position(newPosition)
                .number(newNumber)
                .countryCode(newPlayer.getCountryCode())
                .playerName(newPlayer.getName())
                .playerDateOfBirth(newPlayer.getDateOfBirth())
                .playerId(newPlayer.getId())
                .build();

        // given
        given(teamPlayerRepository.findById(teamPlayerId)).willReturn(Optional.of(oldTeamPlayer));
        given(teamPlayerRepository.teamHasPlayerWithNumber(teamId, createDto.number())).willReturn(false);
        given(playerService.findEntityById(newPlayerId)).willReturn(newPlayer);
        given(teamPlayerRepository.save(argThat(tp ->
                tp.getTeam().equals(oldTeamPlayer.getTeam()) &&
                        tp.getPlayer().equals(newPlayer) &&
                        tp.getNumber().equals(newNumber) &&
                        tp.getPosition().toString().equals(newPosition)
        ))).willReturn(expectedEntity);

        // when
        var result = teamPlayerService.updateTeamPlayer(teamPlayerId, teamId, createDto);

        // then
        assertEquals(expectedDto.getId(), result.getId());
        assertEquals(expectedDto.getNumber(), result.getNumber());
        assertEquals(expectedDto.getPosition(), result.getPosition());
        assertEquals(expectedDto.getCountryCode(), result.getCountryCode());
        assertEquals(expectedDto.getPlayer().getId(), result.getPlayer().getId());
        assertEquals(expectedDto.getPlayer().getName(), result.getPlayer().getName());
        assertEquals(expectedDto.getPlayer().getDateOfBirth(), result.getPlayer().getDateOfBirth());
    }

    @Test
    @DisplayName("markTeamPlayerAsDeleted calls repository's method and returns correct number of entries marked as deleted")
    public void markTeamPlayerAsDeleted_ProvidedId_CorrectlyCallsMethodAndReturnsCount() {
        var idToDelete = UUID.randomUUID();

        // given
        given(teamPlayerRepository.markTeamPlayerAsDeleted(idToDelete)).willReturn(1);

        // when
        var countDeleted = teamPlayerService.markTeamPlayerAsDeleted(idToDelete);

        // then
        assertEquals(1, countDeleted);
    }
}
