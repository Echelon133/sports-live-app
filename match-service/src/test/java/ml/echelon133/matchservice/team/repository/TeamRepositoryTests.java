package ml.echelon133.matchservice.team.repository;

import ml.echelon133.common.match.MatchResult;
import ml.echelon133.common.match.MatchStatus;
import ml.echelon133.matchservice.team.model.TeamDto;
import ml.echelon133.matchservice.coach.model.Coach;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.match.TestMatch;
import ml.echelon133.matchservice.match.model.ScoreInfo;
import ml.echelon133.matchservice.match.repository.MatchRepository;
import ml.echelon133.matchservice.team.TestTeam;
import ml.echelon133.matchservice.team.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class TeamRepositoryTests {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    @Autowired
    public TeamRepositoryTests(TeamRepository teamRepository, MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    private static void assertEntityAndDtoEqual(Team teamEntity, TeamDto teamDto) {
        // simple fields equal
        assertEquals(teamEntity.getId(), teamDto.getId());
        assertEquals(teamEntity.getName(), teamDto.getName());
        assertEquals(teamEntity.getCrestUrl(), teamDto.getCrestUrl());

        // countries equal
        var teamEntityCountry = teamEntity.getCountry();
        var teamDtoCountry = teamDto.getCountry();
        assertTrue(
                teamEntityCountry.getId().equals(teamDtoCountry.getId()) &&
                teamEntityCountry.getName().equals(teamDtoCountry.getName()) &&
                teamEntityCountry.getCountryCode().equals(teamDtoCountry.getCountryCode())
        );

        // coaches equal
        var teamEntityCoach = teamEntity.getCoach();
        var teamDtoCoach = teamDto.getCoach();
        assertTrue(
                teamEntityCoach.getId().equals(teamDtoCoach.getId()) &&
                teamEntityCoach.getName().equals(teamDtoCoach.getName())
        );
    }

    @Test
    @DisplayName("findTeamById native query finds empty when the team does not exist")
    public void findTeamById_TeamDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        Optional<TeamDto> teamDto = teamRepository.findTeamById(id);

        // then
        assertTrue(teamDto.isEmpty());
    }

    @Test
    @DisplayName("findTeamById native query finds team when the team exists")
    public void findTeamById_TeamExists_IsPresent() {
        var team = teamRepository.save(TestTeam.builder().build());

        // when
        var teamDto = teamRepository.findTeamById(team.getId());

        // then
        assertTrue(teamDto.isPresent());
        assertEntityAndDtoEqual(team, teamDto.get());
    }

    @Test
    @DisplayName("findTeamById native query finds team when the team exists and does not leak deleted country")
    public void findTeamById_TeamExistsAndCountryDeleted_IsPresentAndDoesNotLeakDeletedCountry() {
        var country = new Country("Poland", "PL");
        country.setDeleted(true);
        var team = TestTeam.builder().country(country).build();
        var savedTeam = teamRepository.save(team);

        // when
        var teamDto = teamRepository.findTeamById(savedTeam.getId());

        // then
        assertTrue(teamDto.isPresent());
        assertNull(teamDto.get().getCountry());
    }

    @Test
    @DisplayName("findTeamById native query finds team when the team exists and does not leak deleted coach")
    public void findTeamById_TeamExistsAndCoachDeleted_IsPresentAndDoesNotLeakDeletedCoach() {
        var coach = new Coach("Test");
        coach.setDeleted(true);
        var team = TestTeam.builder().coach(coach).build();
        var savedTeam = teamRepository.save(team);

        // when
        var teamDto = teamRepository.findTeamById(savedTeam.getId());

        // then
        assertTrue(teamDto.isPresent());
        assertNull(teamDto.get().getCoach());
    }

    @Test
    @DisplayName("findTeamById native query does not fetch teams marked as deleted")
    public void findTeamById_TeamMarkedAsDeleted_IsEmpty() {
        var teamToDelete = TestTeam.builder().build();
        teamToDelete.setDeleted(true);
        var saved = teamRepository.save(teamToDelete);

        // when
        var teamDto = teamRepository.findTeamById(saved.getId());

        // then
        assertTrue(teamDto.isEmpty());
    }

    @Test
    @DisplayName("markTeamAsDeleted native query only affects the team with specified id")
    public void markTeamAsDeleted_SpecifiedTeamId_OnlyMarksSpecifiedTeam() {
        var saved = teamRepository.save(TestTeam.builder().name("Test1").build());
        teamRepository.save(TestTeam.builder().name("Test2").build());
        teamRepository.save(TestTeam.builder().name("ASDF").build());

        // when
        var countDeleted = teamRepository.markTeamAsDeleted(saved.getId());
        var team = teamRepository.findTeamById(saved.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(team.isEmpty());
    }

    @Test
    @DisplayName("markTeamAsDeleted native query only affects not deleted teams")
    public void markTeamAsDeleted_TeamAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var teamToDelete = TestTeam.builder().name("Test1").build();
        teamToDelete.setDeleted(true);
        var saved = teamRepository.save(teamToDelete);

        // when
        Integer countDeleted = teamRepository.markTeamAsDeleted(saved.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds results which contain the specified phrase")
    public void findAllByNameContaining_MultipleTeams_OnlyFindsMatchingTeams() {
        teamRepository.save(TestTeam.builder().name("Test").build());
        teamRepository.save(TestTeam.builder().name("Test2").build());
        var saved = teamRepository.save(TestTeam.builder().name("Asdf").build());

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("As", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertEntityAndDtoEqual(saved, result.getContent().get(0));
    }

    @Test
    @DisplayName("findAllByNameContaining native query does not leak deleted country of a team")
    public void findAllByNameContaining_TeamWithDeletedCountry_DoesNotLeakDeletedCountry() {
        var country = new Country("Poland", "PL");
        country.setDeleted(true);
        var team = TestTeam.builder().country(country).build();
        teamRepository.save(team);

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining(team.getName(), Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).getCountry());
    }

    @Test
    @DisplayName("findAllByNameContaining native query does not leak deleted coach of a team")
    public void findAllByNameContaining_TeamWithDeletedCoach_DoesNotLeakDeletedCoach() {
        var coach = new Coach("Test");
        coach.setDeleted(true);
        var team = TestTeam.builder().coach(coach).build();
        teamRepository.save(team);

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining(team.getName(), Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).getCoach());
    }

    @Test
    @DisplayName("findAllByNameContaining native query is case-insensitive")
    public void findAllByNameContaining_MultipleTeams_SearchIsCaseInsensitive() {
        teamRepository.save(TestTeam.builder().name("Test").build());
        teamRepository.save(TestTeam.builder().name("TEST").build());
        teamRepository.save(TestTeam.builder().name("Asdf").build());

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("Tes", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("TEST")));
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("Test")));
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds non-deleted results which contain the specified phrase")
    public void findAllByNameContaining_SomeDeletedTeams_OnlyFindsMatchingNonDeletedTeams() {
        var teamToDelete = TestTeam.builder().name("Test").build();
        teamToDelete.setDeleted(true);
        teamRepository.save(teamToDelete);
        teamRepository.save(TestTeam.builder().name("TEST").build());
        teamRepository.save(TestTeam.builder().name("Asdf").build());

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("Test", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("TEST")));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("test", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page number information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageNumber_UsesPageableInfo() {
        var pageable = PageRequest.ofSize(6).withPage(5);

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("test", pageable);

        // then
        var receivedPageable = result.getPageable();
        assertEquals(6, receivedPageable.getPageSize());
        assertEquals(5, receivedPageable.getPageNumber());
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query does not fetch matches marked as deleted")
    public void findFormEvaluationMatches_MatchMarkedAsDeleted_SizeIsZero() {
        var competitionId = UUID.randomUUID();
        var teamId = UUID.randomUUID();
        var matchToDelete = TestMatch.builder()
                .status(MatchStatus.FINISHED)
                .competitionId(competitionId)
                .homeTeam(TestTeam.builder().id(teamId))
                .deleted(true)
                .build();
        matchRepository.save(matchToDelete);

        // when
        var result = teamRepository.findFormEvaluationMatches(teamId, competitionId);

        // then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query fetches team's home and away matches")
    public void findFormEvaluationMatches_BothHomeAndAway_FindsAll() {
        var teamId = UUID.randomUUID();
        var teamBuilder = TestTeam.builder().id(teamId);
        // re-use the same instance across all matches to avoid integrity exceptions
        var savedTeam = teamRepository.save(teamBuilder.build());
        var competitionId = UUID.randomUUID();

        // home match of team
        var match0 = TestMatch.builder()
                .competitionId(competitionId)
                .homeTeam(savedTeam)
                .status(MatchStatus.FINISHED).build();
        // away match of team
        var match1 = TestMatch.builder()
                .competitionId(competitionId)
                .awayTeam(savedTeam)
                .status(MatchStatus.FINISHED).build();
        matchRepository.saveAll(List.of(match0, match1));

        // when
        var result = teamRepository.findFormEvaluationMatches(teamId, competitionId);

        // then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query fetches only finished matches")
    public void findFormEvaluationMatches_MixedFinishedAndUnfinished_FindsOnlyFinished() {
        var teamId = UUID.randomUUID();
        var teamBuilder = TestTeam.builder().id(teamId);
        // re-use the same instance across all matches to avoid integrity exceptions
        var savedTeam = teamRepository.save(teamBuilder.build());
        var competitionId = UUID.randomUUID();

        // only one match of team that is FINISHED
        var match0 = TestMatch.builder()
                .competitionId(competitionId)
                .homeTeam(savedTeam)
                .status(MatchStatus.FINISHED).build();
        var finishedMatches = List.of(match0);

        // multiple matches of team with all possible statuses (except for FINISHED)
        var unfinishedStatuses = Arrays.stream(MatchStatus.values()).filter(
                s -> !s.equals(MatchStatus.FINISHED) // remove FINISHED from the list of statuses
        );
        var unfinishedMatches = unfinishedStatuses.map(unfinishedStatus ->
            TestMatch.builder().competitionId(competitionId).homeTeam(savedTeam).status(unfinishedStatus).build()
        ).collect(Collectors.toList());

        var allMatches = Stream
                .of(finishedMatches, unfinishedMatches).flatMap(List::stream).collect(Collectors.toList());
        matchRepository.saveAll(allMatches);

        // when
        var result = teamRepository.findFormEvaluationMatches(teamId, competitionId);

        // then
        assertEquals(1, result.size());
        var matchDetails = result.get(0);
        // the only finished match is match0
        assertEquals(match0.getId(), matchDetails.getId());
        assertEquals(teamId, matchDetails.getHomeTeam().getId());
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query only fetches matches from the specified competition and team")
    public void findFormEvaluationMatches_MultipleCompetitionsAndTeams_OnlyFetchesSpecificResults() {
        var team0Id = UUID.randomUUID();
        var team0Name = "Team 0";
        var team0Builder = TestTeam.builder().id(team0Id).name(team0Name);

        var team1Id = UUID.randomUUID();
        var team1Name = "Team 1";
        var team1Builder = TestTeam.builder().id(team1Id).name(team1Name);

        var team2Id = UUID.randomUUID();
        var team2Name = "Team 2";
        var team2Builder = TestTeam.builder().id(team2Id).name(team2Name);

        var competition0Id = UUID.randomUUID();
        var competition1Id = UUID.randomUUID();

        // match in which team0 plays in competition0
        var match0 = TestMatch.builder()
                        .competitionId(competition0Id)
                        .homeTeam(team0Builder)
                        .status(MatchStatus.FINISHED).build();
        // match in which team1 plays in competition0
        var match1 = TestMatch.builder()
                        .competitionId(competition0Id)
                        .homeTeam(team1Builder)
                        .status(MatchStatus.FINISHED).build();
        // match in which team2 plays in competition1
        var match2 = TestMatch.builder()
                        .competitionId(competition1Id)
                        .homeTeam(team2Builder)
                        .status(MatchStatus.FINISHED).build();
        matchRepository.saveAll(List.of(match0, match1, match2));

        // when
        var result0 = teamRepository.findFormEvaluationMatches(team0Id, competition0Id);
        var result1 = teamRepository.findFormEvaluationMatches(team1Id, competition0Id);
        var result2 = teamRepository.findFormEvaluationMatches(team2Id, competition1Id);

        // then
        // scenario 1 - team0 plays in competition0
        assertEquals(1, result0.size());
        var match0Details = result0.get(0);
        assertEquals(team0Id, match0Details.getHomeTeam().getId());
        assertEquals(team0Name, match0Details.getHomeTeam().getName());

        // scenario 2 - team1 plays in competition0
        assertEquals(1, result1.size());
        var match1Details = result1.get(0);
        assertEquals(team1Id, match1Details.getHomeTeam().getId());
        assertEquals(team1Name, match1Details.getHomeTeam().getName());

        // scenario 3 - team2 plays in competition1
        assertEquals(1, result2.size());
        var match2Details = result2.get(0);
        assertEquals(team2Id, match2Details.getHomeTeam().getId());
        assertEquals(team2Name, match2Details.getHomeTeam().getName());
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query does not leak deleted home team of a found match")
    public void findFormEvaluationMatches_HomeTeamDeleted_DoesNotLeakDeletedEntity() {
        var competitionId = UUID.randomUUID();
        var teamId = UUID.randomUUID();
        var teamBuilder = TestTeam.builder().id(teamId).deleted(true);
        var match = TestMatch.builder()
                .competitionId(competitionId)
                .homeTeam(teamBuilder)
                .status(MatchStatus.FINISHED)
                .build();
        matchRepository.save(match);

        // when
        var result = teamRepository.findFormEvaluationMatches(teamId, competitionId);

        // then
        assertEquals(1, result.size());
        // make sure that the home team is definitely null
        assertNull(result.get(0).getHomeTeam());
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query does not leak deleted away team of a found match")
    public void findFormEvaluationMatches_AwayTeamDeleted_DoesNotLeakDeletedEntity() {
        var competitionId = UUID.randomUUID();
        var teamId = UUID.randomUUID();
        var teamBuilder = TestTeam.builder().id(teamId).deleted(true);
        var match = TestMatch.builder()
                .competitionId(competitionId)
                .awayTeam(teamBuilder)
                .status(MatchStatus.FINISHED)
                .build();
        matchRepository.save(match);

        // when
        var result = teamRepository.findFormEvaluationMatches(teamId, competitionId);

        // then
        assertEquals(1, result.size());
        // make sure that the away team is definitely null
        assertNull(result.get(0).getAwayTeam());
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query does not fetch more than 5 matches")
    public void findFormEvaluationMatches_MoreThanFiveMatches_FetchesOnlyFive() {
        var teamId = UUID.randomUUID();
        var teamBuilder = TestTeam.builder().id(teamId);
        // re-use the same instance across all matches to avoid integrity exceptions
        var savedTeam = teamRepository.save(teamBuilder.build());
        var competitionId = UUID.randomUUID();

        // create 10 finished matches played by team
        var matches = IntStream.range(0, 10).mapToObj(i ->
            TestMatch.builder().competitionId(competitionId).homeTeam(savedTeam).status(MatchStatus.FINISHED).build()
        ).collect(Collectors.toList());
        matchRepository.saveAll(matches);

        // when
        var result = teamRepository.findFormEvaluationMatches(teamId, competitionId);

        // then
        assertEquals(5, result.size());
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query sorts matches by their start time descending")
    public void findFormEvaluationMatches_DifferentMatchStartTimes_SortsByStartTimeDescending() {
        var teamId = UUID.randomUUID();
        var teamBuilder = TestTeam.builder().id(teamId);
        // re-use the same instance across all matches to avoid integrity exceptions
        var savedTeam = teamRepository.save(teamBuilder.build());
        var competitionId = UUID.randomUUID();

        var startTimes = List.of(
                LocalDateTime.of(2023, 9, 28, 20, 0),
                LocalDateTime.of(2020, 5, 9, 21, 0),
                LocalDateTime.of(2024, 12, 2, 19, 0),
                LocalDateTime.of(2024, 1, 1, 20, 0),
                LocalDateTime.of(2024, 8, 20, 4, 0)
        );
        // reverse order is expected, so matches from 2024 should be first, then 2023, etc.
        var expectedStartTimesOrdering = startTimes.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        var matches = startTimes.stream().map(startTime ->
            TestMatch.builder()
                    .competitionId(competitionId)
                    .homeTeam(savedTeam)
                    .startTimeUTC(startTime)
                    .status(MatchStatus.FINISHED).build()
        ).collect(Collectors.toList());
        matchRepository.saveAll(matches);

        // when
        var result = teamRepository.findFormEvaluationMatches(teamId, competitionId);

        // then
        for (int i = 0; i < result.size(); i++) {
            var match = result.get(i);
            var expectedStartTime = expectedStartTimesOrdering.get(i);
            assertEquals(expectedStartTime, match.getStartTimeUTC());
        }
    }

    @Test
    @DisplayName("findFormEvaluationMatches native query fetches all expected fields")
    public void findFormEvaluationMatches_OneMatch_FetchesAllExpectedFields() {
        var teamA = TestTeam.builder().name("Team A").build();
        var teamB = TestTeam.builder().name("Team B").build();
        var competitionId = UUID.randomUUID();

        var match = TestMatch.builder()
                .competitionId(competitionId)
                .homeTeam(teamA)
                .awayTeam(teamB)
                .scoreInfo(ScoreInfo.of(3, 2))
                .result(MatchResult.HOME_WIN)
                .status(MatchStatus.FINISHED).build();
        matchRepository.save(match);

        // when
        var result = teamRepository.findFormEvaluationMatches(teamA.getId(), competitionId);

        // then
        assertEquals(1, result.size());
        var matchDetails = result.get(0);
        assertEquals(match.getId(), matchDetails.getId());

        assertEquals(teamA.getId(), matchDetails.getHomeTeam().getId());
        assertEquals(teamA.getName(), matchDetails.getHomeTeam().getName());

        assertEquals(teamB.getId(), matchDetails.getAwayTeam().getId());
        assertEquals(teamB.getName(), matchDetails.getAwayTeam().getName());

        assertEquals(3, matchDetails.getScoreInfo().getHomeGoals());
        assertEquals(2, matchDetails.getScoreInfo().getAwayGoals());

        assertEquals(MatchResult.HOME_WIN, matchDetails.getResult());
    }
}
