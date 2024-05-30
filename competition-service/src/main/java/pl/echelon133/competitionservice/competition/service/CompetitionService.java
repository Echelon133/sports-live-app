package pl.echelon133.competitionservice.competition.service;

import pl.echelon133.competitionservice.competition.model.CompetitionDto;
import ml.echelon133.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.echelon133.competitionservice.competition.exceptions.CompetitionInvalidException;
import pl.echelon133.competitionservice.competition.model.*;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Service
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final AsyncMatchServiceClient matchServiceClient;

    @Autowired
    public CompetitionService(CompetitionRepository competitionRepository, AsyncMatchServiceClient matchServiceClient) {
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
     * Creates a competition's entry in the database.
     *
     * The values in {@link UpsertCompetitionDto} have to be pre-validated before being used here,
     * otherwise incorrect data will be placed into the database.
     *
     * @param competitionDto dto representing the information about a competition that will be saved in the database
     * @return id of the created competition
     * @throws CompetitionInvalidException thrown when a competition could not be created based on given data
     */
    public UUID createCompetition(UpsertCompetitionDto competitionDto) throws CompetitionInvalidException {
        var competition = new Competition(
                competitionDto.getName(),
                competitionDto.getSeason(),
                competitionDto.getLogoUrl()
        );

        List<Group> groups = new ArrayList<>(competitionDto.getGroups().size());

        // none of `UUID.fromString` calls should fail because all ids are pre-validated
        List<UUID> teamIds = competitionDto
                .getGroups().stream()
                .flatMap(g -> g.getTeams().stream())
                .map(UUID::fromString)
                .collect(Collectors.toList());

        Map<UUID, List<TeamDetailsDto>> fetchedTeamDetails;
        try {
             fetchedTeamDetails = matchServiceClient.getAllTeams(teamIds);
        } catch (CompletionException ex) {
            throw new CompetitionInvalidException(ex.getMessage());
        }

        for (var groupDto : competitionDto.getGroups()) {
            // none of `UUID.fromString` calls during `map` should fail because all ids are pre-validated
            var teams = groupDto.getTeams().stream().map(UUID::fromString).map(
                    tId -> {
                        // if `getAllTeams` hasn't thrown, `get` is guaranteed to always have a value,
                        // additionally, each value is a list of one element, so we can unpack the value from the list
                        // right away
                        var teamDetail = fetchedTeamDetails.get(tId).get(0);
                        return new TeamStats(teamDetail.getId(), teamDetail.getName(), teamDetail.getCrestUrl());
                    }).collect(Collectors.toList());

            var group = new Group(groupDto.getName(), teams);
            // bidirectionally link teams with their groups
            teams.forEach(t -> t.setGroup(group));
            // bidirectionally link groups with their competition
            group.setCompetition(competition);
            groups.add(group);
        }

        var legend = competitionDto.getLegend().stream().map(
                l ->
                        new Legend(
                                l.getPositions(),
                                l.getContext(),
                                // this `valueOfCaseIgnore` should never fail because the sentiment value is pre-validated
                                Legend.LegendSentiment.valueOfCaseIgnore(l.getSentiment())
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
