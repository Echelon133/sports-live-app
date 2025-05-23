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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
@Transactional
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final MatchServiceClient matchServiceClient;
    private final UnassignedMatchRepository unassignedMatchRepository;
    private final LeagueSlotRepository leagueSlotRepository;
    private final Executor asyncExecutor;

    @Autowired
    public CompetitionService(
            CompetitionRepository competitionRepository,
            MatchServiceClient matchServiceClient,
            UnassignedMatchRepository unassignedMatchRepository,
            LeagueSlotRepository leagueSlotRepository,
            Executor asyncExecutor
    ) {
        this.competitionRepository = competitionRepository;
        this.matchServiceClient = matchServiceClient;
        this.unassignedMatchRepository = unassignedMatchRepository;
        this.leagueSlotRepository = leagueSlotRepository;
        this.asyncExecutor = asyncExecutor;
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
     * Returns the entity representing a competition with the specified id.
     * @param competitionId id of the competition's entity
     * @return competition's entity
     * @throws ResourceNotFoundException thrown when the competition does not exist in the database or is deleted
     */
    public Competition findEntityById(UUID competitionId) throws ResourceNotFoundException {
        return competitionRepository
                .findById(competitionId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Competition.class, competitionId));
    }

    /**
     * Labels all matches from a given competition, filtered by their 'finished' status.
     * <p>
     *     Matches from the league phase are labeled with their round number (i.e. "1", "38", etc.), whereas
     *     matches from the knockout phase are labeled with the name of the stage they belong to
     *     (i.e. "ROUND_OF_16", "FINAL", etc.).
     * </p>
     * @param competitionId id of the competition whose matches we want to fetch and label
     * @param finished if set to `true`, all results will be matches that are finished
     * @param pageable information about the wanted page
     * @return lists of matches labeled by their round or stage name (depending on the competition phase they are from)
     */
    public Map<String, List<CompactMatchDto>> findLabeledMatches(UUID competitionId, boolean finished, Pageable pageable) {
        var labeledMatches = competitionRepository
                .findMatchesLabeledByRoundOrStage(competitionId, finished, pageable)
                .getContent()
                .stream().collect(groupingBy(LabeledMatch::getLabel));

        return labeledMatches
                .entrySet()
                .parallelStream()
                .map(e -> {
                    var matchIdsToFetch = e.getValue().stream().map(LabeledMatch::getMatchId).toList();
                    var fetchedMatches = matchServiceClient.getMatchesById(matchIdsToFetch);
                    // make sure that matches that start first are at the top of the match list
                    fetchedMatches.sort(Comparator.comparing(CompactMatchDto::startTimeUTC));
                    return Map.of(e.getKey(), fetchedMatches);
                })
                .flatMap(m -> m.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
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
     * @param competition competition to check
     * @throws CompetitionPhaseNotFoundException thrown when a competition does not have a league phase
     */
    private void throwIfLeaguePhaseNotSupported(Competition competition) throws CompetitionPhaseNotFoundException {
        if (competition.getLeaguePhase() == null) {
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

        var competition = findEntityById(competitionId);
        throwIfLeaguePhaseNotSupported(competition);
        var maxRounds = competition.getLeaguePhase().getMaxRounds();
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

        var competition = findEntityById(competitionId);
        throwIfLeaguePhaseNotSupported(competition);
        var maxRounds = competition.getLeaguePhase().getMaxRounds();
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
            var competitionMatch = new CompetitionMatch(unassignedMatch.getId().getMatchId(), unassignedMatch.isFinished());
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
     * Finds all matches from the knockout phase of a competition.
     * @param competitionId id of the competition
     * @return an object representing all stages of the knockout phase
     * @throws ResourceNotFoundException thrown when a competition with given id does not exist
     * @throws CompetitionPhaseNotFoundException thrown when a competition does not have a knockout phase
     */
    public KnockoutPhaseDto findKnockoutPhase(UUID competitionId)
            throws ResourceNotFoundException, CompetitionPhaseNotFoundException, CompletionException {
        var competition = findEntityById(competitionId);
        var knockoutPhase = competition.getKnockoutPhase();
        if (knockoutPhase == null) {
            throw new CompetitionPhaseNotFoundException();
        }

        var stages = knockoutPhase.getStages();

        List<UUID> matchIdsToFetch = new ArrayList<>(16);
        List<UUID> teamIdsToFetch = new ArrayList<>(16);

        // traverse the slots of all stages and collect matchIds and teamIds to fetch
        for (var stage : stages) {
            for (var slot : stage.getSlots()) {
                switch (slot) {
                    case KnockoutSlot.Empty ignored -> {}
                    case KnockoutSlot.Bye bye -> teamIdsToFetch.add(bye.getTeamId());
                    case KnockoutSlot.Taken taken -> {
                        matchIdsToFetch.addAll(taken.getLegs().stream().map(CompetitionMatch::getMatchId).toList());
                    }
                    // KnockoutSlot is an entity, therefore it cannot be a sealed class (Hibernate does not allow that),
                    // that's why this default clause has to be here
                    default -> throw new IllegalStateException("Unexpected value: " + slot);
                }
            }
        }

        CompletableFuture<Map<UUID, CompactMatchDto>> fetchedMatchesFuture;
        if (matchIdsToFetch.isEmpty()) {
            fetchedMatchesFuture = CompletableFuture.completedFuture(Map.of());
        } else {
            fetchedMatchesFuture = CompletableFuture.supplyAsync(
                    () -> matchServiceClient
                            .getMatchesById(matchIdsToFetch)
                            .stream()
                            .collect(toMap(CompactMatchDto::id, Function.identity())),
                    asyncExecutor);
        }

        CompletableFuture<Map<UUID, TeamDetailsDto>> fetchedTeamDetailsFuture;
        if (teamIdsToFetch.isEmpty()) {
            fetchedTeamDetailsFuture = CompletableFuture.completedFuture(Map.of());
        } else {
            fetchedTeamDetailsFuture = CompletableFuture.supplyAsync(
                    () -> matchServiceClient
                            .getTeamByTeamIds(teamIdsToFetch, Pageable.ofSize(teamIdsToFetch.size()))
                            .getContent()
                            .stream()
                            .collect(toMap(TeamDetailsDto::id, Function.identity())),
                    asyncExecutor);
        }

        var result = CompletableFuture.allOf(fetchedMatchesFuture, fetchedTeamDetailsFuture);
        // await both futures to complete
        result.join();
        // if we are here, both joins will be instantaneous
        var fetchedMatches = fetchedMatchesFuture.join();
        var fetchedTeamDetails = fetchedTeamDetailsFuture.join();

        // pre-allocate the maximum number of stages expected
        List<KnockoutPhaseDto.StageDto> stageDtos = new ArrayList<>(KnockoutStage.values().length);

        // reconstruct all stages as DTOs, replacing ids referencing matches/teams with data representing these objects
        for (var stage : stages) {
            List<KnockoutPhaseDto.KnockoutSlotDto> slotDtos = new ArrayList<>(16);
            for (var slot : stage.getSlots()) {
                switch (slot) {
                    case KnockoutSlot.Empty ignored -> slotDtos.add(new KnockoutPhaseDto.EmptySlotDto());
                    case KnockoutSlot.Bye bye -> {
                        var teamDetails = fetchedTeamDetails.get(bye.getTeamId());
                        slotDtos.add(new KnockoutPhaseDto.ByeSlotDto(teamDetails));
                    }
                    case KnockoutSlot.Taken taken -> {
                        // first leg is always guaranteed to be present
                        CompactMatchDto firstLeg = fetchedMatches.get(taken.getLegs().get(0).getMatchId());
                        CompactMatchDto secondLeg = null;
                        if (taken.getLegs().size() == 2) {
                            secondLeg = fetchedMatches.get(taken.getLegs().get(1).getMatchId());
                        }
                        slotDtos.add(new KnockoutPhaseDto.TakenSlotDto(firstLeg, secondLeg));
                    }
                    // KnockoutSlot is an entity, therefore it cannot be a sealed class (Hibernate does not allow that),
                    // that's why this default clause has to be here
                    default -> throw new IllegalStateException("Unexpected value: " + slot);
                }
            }
            var stageDto = new KnockoutPhaseDto.StageDto(stage.getStage().name(), slotDtos);
            stageDtos.add(stageDto);
        }

        return new KnockoutPhaseDto(stageDtos);
    }

    /**
     * Updates the knockout tree of a competition based on the given information provided by the client.
     * <p>
     *     {@link UpsertKnockoutTreeDto} has to be validated prior to being passed to this method, otherwise
     *     the state of the knockout tree in the database might get corrupted.
     * </p>
     * @param competitionId id of the competition
     * @param upsertKnockoutTreeDto a tree containing information needed to update the knockout tree
     * @throws ResourceNotFoundException thrown when a competition with given id does not exist
     * @throws CompetitionPhaseNotFoundException thrown when a competition does not have a knockout phase
     * @throws CompetitionMatchAssignmentException thrown when some matches could not be assigned to the knockout tree
     */
    public void updateKnockoutPhase(UUID competitionId, UpsertKnockoutTreeDto upsertKnockoutTreeDto)
            throws ResourceNotFoundException, CompetitionPhaseNotFoundException, CompetitionMatchAssignmentException {

        var competition = findEntityById(competitionId);
        var knockoutPhase = competition.getKnockoutPhase();
        if (knockoutPhase == null) {
            throw new CompetitionPhaseNotFoundException();
        }

        // order the stages in the tree by number of slots descending, since we want these stages ordered left-to-right
        // the way they are actually being played in the competition (with the FINAL stage being the last stage)
        var sortedUpsertKnockoutTree = upsertKnockoutTreeDto
                .stages()
                .stream().sorted(Comparator.comparingInt(stage -> -stage.slots().size()))
                .toList();

        // check if the knockout tree in the database and the knockout tree with update information start on the
        // same stage (i.e. if the tree in the database has a QUARTER_FINAL, SEMI_FINAL, and FINAL, then trees
        // which do not have EXACTLY these stages should be rejected)
        var biggestStageFromUpsert = sortedUpsertKnockoutTree.get(0).stage();
        var biggestStageFromDatabase = knockoutPhase.getStages().get(0).getStage().name();
        if (!biggestStageFromUpsert.equals(biggestStageFromDatabase)) {
            var errorMsg = String.format(
                    "cannot update the knockout tree - updated tree starts at %s, existing tree starts at %s",
                    biggestStageFromUpsert,
                    biggestStageFromDatabase
            );
            throw new CompetitionMatchAssignmentException(errorMsg);
        }

        // we need to know the ids of matches which were already present in the knockout tree prior to it's update,
        // so that any removed matchId can be marked as an unassigned match and returned to the pool of matches
        // that can be reassigned to a league/knockout phase of a competition
        Set<UUID> matchIdsAssignedPriorToUpdate = knockoutPhase
                .getStages()
                .stream().flatMap(stage -> stage.getSlots().stream())
                .filter(slot -> slot instanceof KnockoutSlot.Taken)
                .map(slot -> (KnockoutSlot.Taken)slot)
                .flatMap(taken -> taken.getLegs().stream())
                .map(CompetitionMatch::getMatchId)
                .collect(Collectors.toSet());
        // collect all matchIds which are contained in the updated tree
        Set<UUID> matchIdsInUpdate = upsertKnockoutTreeDto
                .stages()
                .stream().flatMap(stage -> stage.slots().stream())
                .filter(slot -> slot instanceof UpsertKnockoutTreeDto.Taken)
                .map(slot -> (UpsertKnockoutTreeDto.Taken)slot)
                .flatMap(taken -> Stream.of(taken.firstLeg(), taken.secondLeg()))
                // secondLeg could be null, in that case we need to filter it out
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // find all matchIds which do not appear in the updated version of the tree, so that they can be marked
        // as unassigned, so that they are available for assignment later
        Set<UUID> matchIdsToUnassign = new HashSet<>(matchIdsAssignedPriorToUpdate);
        matchIdsToUnassign.removeAll(matchIdsInUpdate);

        // find all matchIds which appear in the updated version but do not appear in the database version of the
        // knockout tree, so that these matches can be marked as assigned
        Set<UUID> matchIdsToAssign = new HashSet<>(matchIdsInUpdate);
        matchIdsToAssign.removeAll(matchIdsAssignedPriorToUpdate);

        // assign all new matches (do not touch matches which are already assigned to this knockout tree)
        var matchesToAssign = unassignedMatchRepository.findAllByIdIsInAndAssignedFalse(
                matchIdsToAssign
                        .stream().map(mId -> new UnassignedMatch.UnassignedMatchId(mId, competitionId))
                        .toList()
                );

        // if the sizes are different, then at least one of the matchIds being inserted into the knockout tree
        // does not actually refer to a match that can be assigned, i.e.:
        //      * the match does not exist at all
        //      * the match might exist, but does not belong to this competition
        if (matchesToAssign.size() != matchIdsToAssign.size()) {
            var receivedMatchIds = matchesToAssign.stream().map(m -> m.getId().getMatchId()).collect(Collectors.toSet());
            var missingMatchIds = new HashSet<>(matchIdsToAssign);
            missingMatchIds.removeAll(receivedMatchIds);
            throw new CompetitionMatchAssignmentException(missingMatchIds.stream().toList());
        }

        // if there were exactly as many matches ready to assign as expected, assign them
        matchesToAssign.forEach(m -> m.setAssigned(true));

        // unassign all matches which should NOT appear in the knockout tree after the update
        var matchesToUnassign = unassignedMatchRepository.findAllById(
                matchIdsToUnassign
                        .stream().map(mId -> new UnassignedMatch.UnassignedMatchId(mId, competitionId))
                        .toList()
                );
        matchesToUnassign.forEach(m -> m.setAssigned(false));

        // save all updates of assigned and unassigned matches
        var assignedAndUnassignedMatches = Stream.concat(matchesToAssign.stream(), matchesToUnassign.stream()).toList();
        unassignedMatchRepository.saveAll(assignedAndUnassignedMatches);

        // store information about matchIds and their status (are they finished or not) so that it can be reused
        // while constructing the updated version of the knockout tree
        Map<UUID, Boolean> matchFinishedCache = assignedAndUnassignedMatches
                .stream().collect(toMap(um -> um.getId().getMatchId(), UnassignedMatch::isFinished));

        // traverse both trees slot-by-slot and update the database version of the tree
        var stagesSize = sortedUpsertKnockoutTree.size();
        for (var stageIndex = 0; stageIndex < stagesSize; stageIndex++) {
            var stageEntity = knockoutPhase.getStages().get(stageIndex);
            var upsertStage = sortedUpsertKnockoutTree.get(stageIndex);
            for (var slotIndex = 0; slotIndex < stageEntity.getStage().getSlots(); slotIndex++) {
                var upsertSlot = upsertStage.slots().get(slotIndex);
                var updatedKnockoutSlot = switch (upsertSlot) {
                    case UpsertKnockoutTreeDto.Empty ignored -> new KnockoutSlot.Empty();
                    case UpsertKnockoutTreeDto.Bye bye -> new KnockoutSlot.Bye(bye.teamId());
                    case UpsertKnockoutTreeDto.Taken taken -> {
                        CompetitionMatch firstLeg = null;
                        CompetitionMatch secondLeg = null;
                        if (taken.firstLeg() != null) {
                            boolean finished = matchFinishedCache.getOrDefault(taken.firstLeg(), false);
                            firstLeg = new CompetitionMatch(taken.firstLeg(), finished);
                        }
                        if (taken.secondLeg() != null) {
                            boolean finished = matchFinishedCache.getOrDefault(taken.secondLeg(), false);
                            secondLeg = new CompetitionMatch(taken.secondLeg(), finished);
                        }
                        yield new KnockoutSlot.Taken(firstLeg, secondLeg);
                    }
                };
                stageEntity.getSlots().set(slotIndex, updatedKnockoutSlot);
            }
        }
        competitionRepository.save(competition);
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
