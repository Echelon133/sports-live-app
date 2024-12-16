package pl.echelon133.competitionservice.competition.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.echelon133.competitionservice.competition.client.MatchServiceClient;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionInvalidException;
import pl.echelon133.competitionservice.competition.model.*;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;

import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final MatchServiceClient matchServiceClient;

    @Autowired
    public CompetitionService(CompetitionRepository competitionRepository, MatchServiceClient matchServiceClient) {
        this.competitionRepository = competitionRepository;
        this.matchServiceClient = matchServiceClient;
    }

    /**
     * Returns the information about the competition with specified id.
     *
     * @param competitionId id of the competition
     * @return a dto representing the competition
     * @throws ResourceNotFoundException thrown when the competition does not exist in the database
     */
    public CompetitionDto findById(UUID competitionId) throws ResourceNotFoundException {
        return competitionRepository
                .findCompetitionById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException(Competition.class, competitionId));
    }

    /**
     * Marks a competition with the specified id as deleted.
     *
     * @param id id of the competition to be marked as deleted
     * @return how many entities have been affected
     */
    public Integer markCompetitionAsDeleted(UUID id)  {
        return competitionRepository.markCompetitionAsDeleted(id);
    }

    /**
     * Finds all competitions whose names contain the specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the competition
     * @param pageable information about the wanted page
     * @return a page of competitions which match the filter
     */
    public Page<CompetitionDto> findCompetitionsByName(String phrase, Pageable pageable) {
        return competitionRepository.findAllByNameContaining(phrase, pageable);
    }

    /**
     * Finds all non-deleted competitions which are marked as <i>pinned</i>.
     *
     * @return a list of non-deleted competitions which are marked as "pinned"
     */
    public List<CompetitionDto> findPinnedCompetitions() {
        return competitionRepository.findAllPinned();
    }

    /**
     * Creates a competition's entry in the database.
     *
     * The values in {@link UpsertCompetitionDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param competitionDto dto representing the information about a competition that will be saved in the database
     * @return id of the created competition
     * @throws CompetitionInvalidException thrown when a competition could not be created using given data
     */
    public UUID createCompetition(UpsertCompetitionDto competitionDto) throws CompetitionInvalidException {
        var competition = new Competition(
                competitionDto.name(),
                competitionDto.season(),
                competitionDto.logoUrl()
        );
        competition.setPinned(competitionDto.pinned());

        List<Group> groups = new ArrayList<>(competitionDto.groups().size());

        // none of `UUID.fromString` calls should fail because all ids are pre-validated
        List<UUID> requestedTeamIds = competitionDto
                .groups().stream()
                .flatMap(g -> g.teams().stream())
                .map(UUID::fromString)
                .collect(Collectors.toList());

        // fetch details about teams which are supposed to play in this competition, and group them by
        // their id for faster lookup (Upsert object is pre-validated and makes sure there are no duplicate ids,
        // therefore every list should be of size 1)
        Map<UUID, List<TeamDetailsDto>> allTeamDetails = matchServiceClient
                .getTeamByTeamIds(requestedTeamIds, Pageable.ofSize(requestedTeamIds.size()))
                .getContent()
                .stream()
                .collect(groupingBy(TeamDetailsDto::id));

        // only proceed with creating this competition if there are exactly as many fetched team details as requested,
        // otherwise there is a possibility that:
        //      * some of the ids do not refer to teams that actually exist
        //      * the API endpoint does not fetch details about existing teams correctly
        if (allTeamDetails.size() != requestedTeamIds.size()) {
            var allRequestedIds = new HashSet<>(requestedTeamIds);
            var allReceivedIds = allTeamDetails.keySet();

            if (allReceivedIds.size() > allRequestedIds.size()) {
                // this will only happen if the API endpoint is broken and returns more teams than requested,
                // ideally we should return error 500
                throw new RuntimeException("validation of teams' data could not be completed successfully");
            } else {
                // evaluate which team ids did not return a result, so that the missing ids can be displayed in the
                // error message
                allRequestedIds.removeAll(allReceivedIds);

                var formattedMessage = String.format(
                        "teams with ids %s cannot be placed in this competition",
                        Arrays.toString(allRequestedIds.toArray())
                );
                throw new CompetitionInvalidException(formattedMessage);
            }
        }

        for (var groupDto : competitionDto.groups()) {
            // none of `UUID.fromString` calls during `map` should fail because all ids are pre-validated
            var teams = groupDto.teams().stream().map(UUID::fromString).map(
                    tId -> {
                        // at this point, it's GUARANTEED that there is a team details object with that id at index 0:
                        //      * if such id had not been found, an exception would have been thrown before getting here
                        //      * if it has been found, then the team details object corresponding to it exists and has
                        //          been placed in the lookup map in a list which contains exactly 1 element
                        var teamDetail = allTeamDetails.get(tId).get(0);
                        return new TeamStats(teamDetail.id(), teamDetail.name(), teamDetail.crestUrl());
                    }).collect(Collectors.toList());

            var group = new Group(groupDto.name(), teams);
            // bidirectionally link teams with their groups
            teams.forEach(t -> t.setGroup(group));
            // bidirectionally link groups with their competition
            group.setCompetition(competition);
            groups.add(group);
        }

        var legend = competitionDto.legend().stream().map(
                l ->
                        new Legend(
                                l.positions(),
                                l.context(),
                                // this `valueOfIgnoreCase` should never fail because the sentiment value is pre-validated
                                Legend.LegendSentiment.valueOfIgnoreCase(l.sentiment())
                        )
                ).collect(Collectors.toList());

        competition.setGroups(groups);
        competition.setLegend(legend);
        return competitionRepository.save(competition).getId();
    }

    /**
     * Returns the information about the standings in competition with specified id.
     * @param competitionId id of the competition
     * @return a dto representing the standings in that competition
     * @throws ResourceNotFoundException thrown when the competition does not exist in the database
     */
    public StandingsDto findStandings(UUID competitionId) throws ResourceNotFoundException {
        var competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException(Competition.class, competitionId));
        return convertCompetitionEntityToStandingsDto(competition);
    }

    private static StandingsDto convertCompetitionEntityToStandingsDto(Competition entity) {
        // convert groups
        List<StandingsDto.GroupDto> groupDtos = entity.getGroups().stream().parallel().map(groupEntity -> {
            List<StandingsDto.TeamStatsDto> teamDtos = groupEntity.getTeams().stream()
                    .map(StandingsDto.TeamStatsDto::new).collect(Collectors.toList());
            return new StandingsDto.GroupDto(groupEntity.getName(), teamDtos);
        }).collect(Collectors.toList());

        // convert legend
        List<StandingsDto.LegendDto> legendDtos = entity.getLegend().stream().parallel()
                .map(StandingsDto.LegendDto::new)
                .collect(Collectors.toList());

        return new StandingsDto(groupDtos, legendDtos);
    }

    /**
     * Finds all competition-specific statistics of players who play in the specified competition.
     *
     * @param competitionId id of the competition of which the statistics will be fetched
     * @param pageable information about the wanted page
     * @return a page containing player statistics
     */
    public Page<PlayerStatsDto> findPlayerStatsByCompetition(UUID competitionId, Pageable pageable) {
        return competitionRepository.findPlayerStats(competitionId, pageable);
    }
}
