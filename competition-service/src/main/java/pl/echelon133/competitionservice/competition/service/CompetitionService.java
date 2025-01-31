package pl.echelon133.competitionservice.competition.service;

import jakarta.transaction.Transactional;
import ml.echelon133.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.echelon133.competitionservice.competition.client.MatchServiceClient;
import pl.echelon133.competitionservice.competition.exceptions.*;
import pl.echelon133.competitionservice.competition.model.*;
import pl.echelon133.competitionservice.competition.repository.CompetitionRepository;
import pl.echelon133.competitionservice.competition.repository.LeagueSlotRepository;
import pl.echelon133.competitionservice.competition.repository.UnassignedMatchRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final MatchServiceClient matchServiceClient;
    private final UnassignedMatchRepository unassignedMatchRepository;
    private final LeagueSlotRepository leagueSlotRepository;

    @Autowired
    public CompetitionService(
            CompetitionRepository competitionRepository,
            MatchServiceClient matchServiceClient,
            UnassignedMatchRepository unassignedMatchRepository,
            LeagueSlotRepository leagueSlotRepository
    ) {
        this.competitionRepository = competitionRepository;
        this.matchServiceClient = matchServiceClient;
        this.unassignedMatchRepository = unassignedMatchRepository;
        this.leagueSlotRepository = leagueSlotRepository;
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
     * Finds all unassigned matches from a particular competition.
     * <p>
     *     An unassigned match belongs to neither the league nor the knockout phase of a competition.
     *     During the assignment process, the match receives its round (in case of the league phase) or
     *     its stage (in case of the knockout phase).
     * </p>
     *
     * @param competitionId id of the competition whose unassigned matches we want to fetch
     * @param pageable information about the wanted page
     * @return a page of unassigned matches
     */
    public Page<CompactMatchDto> findUnassignedMatches(UUID competitionId, Pageable pageable) {
        var unassignedMatchIds = unassignedMatchRepository
                .findAllById_CompetitionIdAndAssignedFalse(competitionId, pageable)
                .map(um -> um.getId().getMatchId())
                .stream()
                .toList();

        var matchData = matchServiceClient.getMatchesById(unassignedMatchIds);

        // since the ids of unassigned matches map one-to-one to matches returned by this method, the
        // same pageable can be used in the return value
        return new PageImpl<>(matchData, pageable, matchData.size());
    }

    /**
     * Helper method which throws if the particular competition does not have a league phase.
     *
     * @param maxRounds number of rounds in the competition's league phase
     * @throws CompetitionPhaseNotFoundException thrown when a competition does not have a league phase
     */
    private void throwIfLeaguePhaseNotSupported(int maxRounds) throws CompetitionPhaseNotFoundException {
        if (maxRounds == 0) {
            // if maxRounds is 0, then the competition does not support the league phase
            throw new CompetitionPhaseNotFoundException();
        }
    }

    /**
     * Helper method which throws if the particular competition does not have a round represented by a given number.
     *
     * @param round round to check
     * @param maxRounds number of rounds in the competition's league phase
     * @throws CompetitionRoundNotFoundException thrown when a competition does not have a given round in its league phase
     */
    private void throwIfRoundNotFound(int round, int maxRounds) throws CompetitionRoundNotFoundException {
        if (!(1 <= round && round <= maxRounds)) {
            // if round is not in (1, maxRounds), then that round does not exist
            throw new CompetitionRoundNotFoundException(round);
        }
    }

    /**
     * Finds all matches from a particular round of a competition which has a league phase.
     * @param competitionId id of the competition
     * @param round round to which the match belongs
     * @return a list of matches
     * @throws ResourceNotFoundException thrown when a competition with given id does not exist
     * @throws CompetitionPhaseNotFoundException thrown when a competition does not have a league phase
     * @throws CompetitionRoundNotFoundException thrown when a competition does not have a given round in its league phase
     */
    public List<CompactMatchDto> findMatchesByRound(UUID competitionId, int round)
            throws ResourceNotFoundException, CompetitionPhaseNotFoundException, CompetitionRoundNotFoundException {

        var competition = findById(competitionId);
        var maxRounds = competition.getMaxRounds();

        throwIfLeaguePhaseNotSupported(maxRounds);
        throwIfRoundNotFound(round, maxRounds);

        var matchIdsFromRound = leagueSlotRepository
                .findAllByCompetitionIdAndRoundAndDeletedFalse(competitionId, round)
                .stream().map(slot -> slot.getMatch().getMatchId())
                .toList();
        return matchServiceClient.getMatchesById(matchIdsFromRound);
    }

    /**
     * Assigns matches to a particular round of a particular competition.
     * @param competitionId id of the competition
     * @param round round which is being assigned matches
     * @param matchIdsToAssign a list of ids of matches to assign to the round
     * @throws ResourceNotFoundException thrown when a competition with given id does not exist
     * @throws CompetitionPhaseNotFoundException thrown when a competition does not have a league phase
     * @throws CompetitionRoundNotFoundException thrown when a competition does not have a given round in its league phase
     * @throws CompetitionRoundNotEmptyException thrown when a round already has matches assigned to it
     * @throws CompetitionMatchAssignmentException thrown when some matches could not be assigned to the round
     */
    public void assignMatchesToRound(UUID competitionId, int round, List<UUID> matchIdsToAssign)
            throws ResourceNotFoundException, CompetitionPhaseNotFoundException,
            CompetitionRoundNotFoundException, CompetitionRoundNotEmptyException, CompetitionMatchAssignmentException {

        var competition = findById(competitionId);
        var maxRounds = competition.getMaxRounds();

        throwIfLeaguePhaseNotSupported(maxRounds);
        throwIfRoundNotFound(round, maxRounds);

        var matchIdsAlreadyInRound = leagueSlotRepository
                .findAllByCompetitionIdAndRoundAndDeletedFalse(competitionId, round).stream()
                .map(m -> m.getMatch().getMatchId())
                .toList();

        // do not allow assigning matches to this round if there are already matches assigned to it
        if (matchIdsAlreadyInRound.size() != 0) {
            throw new CompetitionRoundNotEmptyException();
        }

        // create composite keys of all requested unassigned matches, and fetch these unassigned matches
        var unassignedMatchesIds = matchIdsToAssign
                .stream().map(id -> new UnassignedMatch.UnassignedMatchId(id, competitionId))
                .toList();
        var retrievedUnassignedMatches = unassignedMatchRepository.findAllByIdIsInAndAssignedFalse(unassignedMatchesIds);

        // some requested matchIds did not refer to matches which are unassigned
        if (unassignedMatchesIds.size() != retrievedUnassignedMatches.size()) {
            // retrieved unassigned matches have composite keys, so extract only matchId from each key
            var retrievedUnassignedMatchesMatchIds = retrievedUnassignedMatches
                    .stream().map(m -> m.getId().getMatchId())
                    .collect(Collectors.toSet());
            // calculate a list of matchIds which were requested to be assigned, but were not in the set of matches
            // available to assign
            var diffMatchIds = matchIdsToAssign.stream().dropWhile(retrievedUnassignedMatchesMatchIds::contains).toList();
            throw new CompetitionMatchAssignmentException(diffMatchIds);
        }

        // assignment is possible here, since all validation was successful
        List<LeagueSlot> leagueSlots = new ArrayList<>(retrievedUnassignedMatches.size());
        for (var unassignedMatch: retrievedUnassignedMatches) {
            var competitionMatch = new CompetitionMatch(unassignedMatch.getId().getMatchId());
            leagueSlots.add(new LeagueSlot(competitionMatch, competitionId, round));
            unassignedMatch.setAssigned(true);
        }
        unassignedMatchRepository.saveAll(retrievedUnassignedMatches);
        leagueSlotRepository.saveAll(leagueSlots);
    }

    /**
     * Unassigns matches which are assigned to a particular round in the given competition.
     * <p>Matches unassigned from a round are eligible to being reassigned to same/different round later.</p>
     *
     * @param competitionId id of the competition whose round we want to unassign
     * @param round round of the competition whose matches we want to unassign
     */
    public void unassignMatchesFromRound(UUID competitionId, int round) {

        var matchesInRound = leagueSlotRepository
                .findAllByCompetitionIdAndRoundAndDeletedFalse(competitionId, round);

        if (matchesInRound.size() == 0) {
            return;
        }

        // at first delete all assignments between matches and their round
        matchesInRound.forEach(m -> m.setDeleted(true));
        leagueSlotRepository.saveAll(matchesInRound);

        // for every match which is no longer assigned to a round, set its assigned flag to `false`
        // so that it's possible to reassign these matches later
        var unassignedMatchIds = matchesInRound
                .stream().map(m -> new UnassignedMatch.UnassignedMatchId(m.getMatch().getMatchId(), m.getCompetitionId()))
                .toList();
        var unassignedMatchEntities = unassignedMatchRepository.findAllById(unassignedMatchIds);
        unassignedMatchEntities.forEach(um -> um.setAssigned(false));
        unassignedMatchRepository.saveAll(unassignedMatchEntities);
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
     * Sets up the league phase of a competition.
     *
     * @param competition competition whose league phase to set up
     * @param leaguePhaseDto dto containing information about how to set up the league phase
     * @return {@link LeaguePhase} or null if there is no league phase to set up
     * @throws CompetitionInvalidException thrown when a competition could not be created using given data
     */
    private LeaguePhase setupLeaguePhase(Competition competition, UpsertCompetitionDto.UpsertLeaguePhaseDto leaguePhaseDto)
            throws CompetitionInvalidException {
        if (leaguePhaseDto == null) {
            return null;
        }

        List<Group> groups = new ArrayList<>(leaguePhaseDto.groups().size());

        // none of `UUID.fromString` calls should fail because all ids are pre-validated
        List<UUID> requestedTeamIds = leaguePhaseDto
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

        for (var groupDto : leaguePhaseDto.groups()) {
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

        var legend = leaguePhaseDto.legend().stream().map(
                l ->
                        new Legend(
                                l.positions(),
                                l.context(),
                                // this `valueOfIgnoreCase` should never fail because the sentiment value is pre-validated
                                Legend.LegendSentiment.valueOfIgnoreCase(l.sentiment())
                        )
        ).collect(Collectors.toList());

        return new LeaguePhase(groups, legend, leaguePhaseDto.maxRounds());
    }

    /**
     * Sets up the knockout phase of a competition.
     *
     * @param knockoutPhaseDto dto containing information about how to set up the knockout phase
     * @return {@link KnockoutPhase} or null if there is no knockout phase to set up
     */
    private KnockoutPhase setupKnockoutPhase(UpsertCompetitionDto.UpsertKnockoutPhaseDto knockoutPhaseDto) {
        if (knockoutPhaseDto == null) {
            return null;
        }

        // ordered stages which should appear in the competition's knockout phase
        // i.e. QUARTER_FINAL, SEMI_FINAL, FINAL, in this order
        var neededStages = KnockoutStage
                .getStagesForCompetition(KnockoutStage.valueOfIgnoreCase(knockoutPhaseDto.startsAt()));
        var stages = neededStages.stream().map(stage -> {
            // initialize every stage with as many empty slots as that stage requires, i.e.
            //      * the round of 16 needs 8 empty slots (8 single/double legged matches)
            //      * the semi-final needs 2 slots (2 single/double legged matches)
            //      * etc...
            var slotsPerStage = stage.getSlots();
            var stageEntity = new Stage(stage);
            List<KnockoutSlot> emptySlots = IntStream
                    .range(0, slotsPerStage)
                    .mapToObj(i -> new KnockoutSlot.Empty())
                    .collect(Collectors.toList());
            stageEntity.setSlots(emptySlots);
            return stageEntity;
        }).toList();
        return new KnockoutPhase(stages);
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
        competition.setLeaguePhase(setupLeaguePhase(competition, competitionDto.leaguePhase()));
        competition.setKnockoutPhase(setupKnockoutPhase(competitionDto.knockoutPhase()));

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
        var leaguePhase = entity.getLeaguePhase();

        // convert groups
        List<StandingsDto.GroupDto> groupDtos = leaguePhase.getGroups().stream().parallel().map(groupEntity -> {
            List<StandingsDto.TeamStatsDto> teamDtos = groupEntity.getTeams().stream()
                    .map(StandingsDto.TeamStatsDto::new).collect(Collectors.toList());
            return new StandingsDto.GroupDto(groupEntity.getName(), teamDtos);
        }).collect(Collectors.toList());

        // convert legend
        List<StandingsDto.LegendDto> legendDtos = leaguePhase.getLegend().stream().parallel()
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
