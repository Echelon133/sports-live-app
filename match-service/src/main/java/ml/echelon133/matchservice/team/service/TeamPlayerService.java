package ml.echelon133.matchservice.team.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.matchservice.team.model.TeamDto;
import ml.echelon133.matchservice.team.model.TeamPlayerDto;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.player.service.PlayerService;
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
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamPlayerService {

    private final TeamPlayerRepository teamPlayerRepository;
    private final TeamRepository teamRepository;
    private final PlayerService playerService;

    @Autowired
    public TeamPlayerService(TeamPlayerRepository teamPlayerRepository,
                             TeamRepository teamRepository,
                             PlayerService playerService) {
        this.teamPlayerRepository = teamPlayerRepository;
        this.teamRepository = teamRepository;
        this.playerService = playerService;
    }

    /**
     * Turns a list of {@link UUID}s of team players into a list of references to {@link TeamPlayer} entities.
     *
     * @param teamPlayerIds a list of ids to be turned into references
     * @return a list of references to entities
     */
    public List<TeamPlayer> mapAllIdsToReferences(List<UUID> teamPlayerIds) {
        return teamPlayerIds.stream().map(teamPlayerRepository::getReferenceById).collect(Collectors.toList());
    }

    /**
     * Returns the entity representing a team player with the specified id.
     * @param id id of the team player's entity
     * @return team player's entity
     * @throws ResourceNotFoundException thrown when the team player does not exist in the database or is deleted
     */
    public TeamPlayer findEntityById(UUID id) throws ResourceNotFoundException {
        return teamPlayerRepository
                .findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(TeamPlayer.class, id));
    }

    /**
     * Finds all players who currently play for the team with specified id.
     *
     * @param teamId id of the team whose players have to be found
     * @return a list of players who play for the team
     */
    public List<TeamPlayerDto> findAllPlayersOfTeam(UUID teamId) {
        return teamPlayerRepository.findAllPlayersByTeamId(teamId);
    }

    /**
     * Finds all teams for which the player with specified id currently plays.
     * @param playerId id of the player whose teams have to be found
     * @return a list of teams for which the player plays
     */
    public List<TeamDto> findAllTeamsOfPlayer(UUID playerId) {
        return teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(playerId);
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
        var player = playerService.findEntityById(playerId);

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
        var player = playerService.findEntityById(playerId);

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
