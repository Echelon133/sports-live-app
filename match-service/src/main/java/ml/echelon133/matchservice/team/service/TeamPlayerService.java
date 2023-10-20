package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.common.team.dto.TeamPlayerDto;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.player.repository.PlayerRepository;
import ml.echelon133.matchservice.team.exception.NumberAlreadyTakenException;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import ml.echelon133.matchservice.team.model.UpsertTeamPlayerDto;
import ml.echelon133.matchservice.team.repository.TeamPlayerRepository;
import ml.echelon133.matchservice.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TeamPlayerService {

    private final TeamPlayerRepository teamPlayerRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public TeamPlayerService(TeamPlayerRepository teamPlayerRepository,
                             TeamRepository teamRepository,
                             PlayerRepository playerRepository) {
        this.teamPlayerRepository = teamPlayerRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Finds all players who currently play for the team with specified id.
     *
     * @param teamId id of the team whose players have to be found
     * @return a list of players who play for the team
     * @throws ResourceNotFoundException thrown when the team with specified id does not exist or was marked as deleted
     */
    public List<TeamPlayerDto> findAllPlayersOfTeam(UUID teamId) throws ResourceNotFoundException {
        if (teamRepository.existsByIdAndDeletedIsFalse(teamId)) {
            return teamPlayerRepository.findAllPlayersByTeamId(teamId);
        }
        throw new ResourceNotFoundException(Team.class, teamId);
    }

    /**
     * Finds all teams for which the player with specified id currently plays.
     * @param playerId id of the player whose teams have to be found
     * @return a list of teams for which the player plays
     * @throws ResourceNotFoundException thrown when the player with specified id does not exist or was marked as deleted
     */
    public List<TeamDto> findAllTeamsOfPlayer(UUID playerId) throws ResourceNotFoundException {
        if (playerRepository.existsByIdAndDeletedFalse(playerId)) {
            return teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(playerId);
        }
        throw new ResourceNotFoundException(Player.class, playerId);
    }

    /**
     * Assigns an existing player to an existing team. A player can belong to multiple teams at the same time
     * (e.g. playing for their club and their national team).
     *
     * The values in {@link UpsertTeamPlayerDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param teamId id of the team to which we are assigning a player
     * @param teamPlayerDto dto containing information about the player
     * @return a dto containing information about the created team player
     * @throws ResourceNotFoundException thrown when the team or player entities do not exist or are marked as deleted
     * @throws NumberAlreadyTakenException thrown when the team already has a player with the specified number
     */
    public TeamPlayerDto createTeamPlayer(UUID teamId, UpsertTeamPlayerDto teamPlayerDto)
            throws ResourceNotFoundException, NumberAlreadyTakenException {

        // check the player's number as quickly as possible, because if the number is taken then
        // there is no point in fetching other data
        if (teamPlayerRepository.teamHasPlayerWithNumber(teamId, teamPlayerDto.getNumber())) {
            throw new NumberAlreadyTakenException(teamId, teamPlayerDto.getNumber());
        }

        // find player's team
        var team = teamRepository
                .findById(teamId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Team.class, teamId));

        // this `UUID.fromString` should never fail because the PlayerId value is pre-validated
        var playerId = UUID.fromString(teamPlayerDto.getPlayerId());
        var player = playerRepository
                .findById(playerId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Player.class, playerId));

        // this `Position.valueOfIgnoreCase` should never fail because the Position value is pre-validated
        var position = Position.valueOfIgnoreCase(teamPlayerDto.getPosition());

        var teamPlayer = new TeamPlayer(team, player, position, teamPlayerDto.getNumber());
        return TeamPlayerMapper.entityToDto(teamPlayerRepository.save(teamPlayer));
    }

    /**
     * Updates an assignment of an existing player to an existing team.
     *
     * The values in {@link UpsertTeamPlayerDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param teamPlayerId id of the player's assignment
     * @param teamId id of the team
     * @param teamPlayerDto dto containing information storing new data about the player
     * @return a dto containing information about the updated player
     * @throws ResourceNotFoundException thrown when the team or player entities do not exist or are marked as deleted
     * @throws NumberAlreadyTakenException thrown when the team already has a player with the specified number
     */
    public TeamPlayerDto updateTeamPlayer(UUID teamPlayerId, UUID teamId, UpsertTeamPlayerDto teamPlayerDto)
            throws ResourceNotFoundException, NumberAlreadyTakenException {

        var teamPlayer =
                teamPlayerRepository
                        .findById(teamPlayerId)
                        .filter(t -> !t.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException(TeamPlayer.class, teamPlayerId));

        // team might be marked as deleted
        if (teamPlayer.getTeam().isDeleted()) {
            throw new ResourceNotFoundException(Team.class, teamId);
        }

        // when the client makes an update request, they have to provide both teamId and teamPlayerId,
        // we need to make sure that the teamPlayer with teamPlayerId actually plays for the team with teamId,
        // and if they do not, we cannot continue because there's been some mistake on the client's part
        if (!teamPlayer.getTeam().getId().equals(teamId)) {
            throw new ResourceNotFoundException(TeamPlayer.class, teamPlayerId);
        }

        // if the player's number is already taken, we cannot continue
        if (teamPlayerRepository.teamHasPlayerWithNumber(teamId, teamPlayerDto.getNumber())) {
            throw new NumberAlreadyTakenException(teamId, teamPlayerDto.getNumber());
        }

        // this `UUID.fromString` should never fail because the PlayerId value is pre-validated
        var playerId = UUID.fromString(teamPlayerDto.getPlayerId());
        var player = playerRepository
                .findById(playerId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Player.class, playerId));

        // this `Position.valueOfIgnoreCase` should never fail because the Position value is pre-validated
        var position = Position.valueOfIgnoreCase(teamPlayerDto.getPosition());

        teamPlayer.setPlayer(player);
        teamPlayer.setPosition(position);
        teamPlayer.setNumber(teamPlayerDto.getNumber());
        return TeamPlayerMapper.entityToDto(teamPlayerRepository.save(teamPlayer));
    }

    /**
     * Marks an assignment between the team and the player as deleted.
     *
     * @param id id of the assignment
     * @return how many entities have been affected
     */
    public Integer markTeamPlayerAsDeleted(UUID id)  {
        return teamPlayerRepository.markTeamPlayerAsDeleted(id);
    }
}
