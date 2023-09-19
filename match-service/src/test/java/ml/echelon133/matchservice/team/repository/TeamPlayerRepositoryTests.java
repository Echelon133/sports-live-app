package ml.echelon133.matchservice.team.repository;


import ml.echelon133.matchservice.coach.model.Coach;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.model.TeamPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class TeamPlayerRepositoryTests {

    private final TeamPlayerRepository teamPlayerRepository;

    @Autowired
    public TeamPlayerRepositoryTests(TeamPlayerRepository teamPlayerRepository) {
        this.teamPlayerRepository = teamPlayerRepository;
    }

    public Team getTestTeam(String teamName) {
        return new Team(
                teamName,
                "https://cdn.test.com/image.png",
                new Country("Test", "TC"),
                new Coach("Test Coach")
        );
    }

    public Player getTestPlayer(String name) {
        return new Player(
                name,
                Position.FORWARD,
                LocalDate.of(1970, 1, 1),
                new Country("Test1", "SC")
        );
    }

    @Test
    @DisplayName("findAllPlayersByTeamId native query finds zero players when the team does not exist")
    public void findAllPlayersByTeamId_TeamDoesNotExist_IsEmpty() {
        var teamId = UUID.randomUUID();

        // when
        var players = teamPlayerRepository.findAllPlayersByTeamId(teamId);

        // then
        assertTrue(players.isEmpty());
    }

    @Test
    @DisplayName("findAllPlayersByTeamId native query finds zero players when the team is marked as deleted")
    public void findAllPlayersByTeamId_TeamMarkedAsDeleted_IsEmpty() {
        var team = getTestTeam("Some team");
        team.setDeleted(true);

        teamPlayerRepository.save(
                new TeamPlayer(team, getTestPlayer("Test1"), Position.GOALKEEPER, 1)
        );
        teamPlayerRepository.save(
                new TeamPlayer(team, getTestPlayer("Test2"), Position.FORWARD, 9)
        );

        // when
        var players = teamPlayerRepository.findAllPlayersByTeamId(team.getId());

        // then
        assertTrue(players.isEmpty());
    }

    @Test
    @DisplayName("findAllPlayersByTeamId native query does not find players who play for the team but are marked as deleted")
    public void findAllPlayersByTeamId_PlayerMarkedAsDeleted_DoesNotContainDeletedPlayer() {
        var team = getTestTeam("Some team");

        var deletedPlayer = getTestPlayer("Test1");
        deletedPlayer.setDeleted(true);
        teamPlayerRepository.save(
                new TeamPlayer(team, deletedPlayer, Position.GOALKEEPER, 1)
        );
        teamPlayerRepository.save(
                new TeamPlayer(team, getTestPlayer("Test2"), Position.FORWARD, 9)
        );

        // when
        var players = teamPlayerRepository.findAllPlayersByTeamId(team.getId());

        // then
        assertEquals(1, players.size());
        assertEquals("Test2", players.get(0).getPlayer().getName());
    }

    @Test
    @DisplayName("findAllPlayersByTeamId native query does not find players whose assignments to the team are marked as deleted")
    public void findAllPlayersByTeamId_TeamPlayerMarkedAsDeleted_DoesNotContainPlayer() {
        var team = getTestTeam("Some team");

        var player = getTestPlayer("Test1");
        var deletedTeamPlayer = new TeamPlayer(team, player, Position.GOALKEEPER, 1);
        deletedTeamPlayer.setDeleted(true);

        teamPlayerRepository.save(deletedTeamPlayer);
        teamPlayerRepository.save(
                new TeamPlayer(team, getTestPlayer("Test2"), Position.FORWARD, 9)
        );

        // when
        var players = teamPlayerRepository.findAllPlayersByTeamId(team.getId());

        // then
        assertEquals(1, players.size());
        assertEquals("Test2", players.get(0).getPlayer().getName());
    }

    @Test
    @DisplayName("findAllPlayersByTeamId native query finds only players who play for the specific team")
    public void findAllPlayersByTeamId_MultipleTeamsWithPlayers_OnlyContainsPlayersFromOneTeam() {
        var team0 = getTestTeam("Team1");
        var team1 = getTestTeam("Team2");

        var teamPlayer0 = teamPlayerRepository.save(
                new TeamPlayer(team0, getTestPlayer("Test1"), Position.GOALKEEPER, 1)
        );
        var teamPlayer1 = teamPlayerRepository.save(
                new TeamPlayer(team1, getTestPlayer("Test2"), Position.FORWARD, 9)
        );

        // when
        var team0Players = teamPlayerRepository.findAllPlayersByTeamId(team0.getId());
        var team1Players = teamPlayerRepository.findAllPlayersByTeamId(team1.getId());

        // then
        assertEquals(1, team0Players.size());
        assertEquals(teamPlayer0.getPlayer().getName(), team0Players.get(0).getPlayer().getName());

        assertEquals(1, team1Players.size());
        assertEquals(teamPlayer1.getPlayer().getName(), team1Players.get(0).getPlayer().getName());
    }

    @Test
    @DisplayName("findAllPlayersByTeamId native query does not leak player's country code if the country was marked as deleted")
    public void findAllPlayersByTeamId_PlayerCountryMarkedAsDeleted_DoesNotLeakDeletedCountry() {
        var team = getTestTeam("Team1");

        var player = getTestPlayer("Test1");
        player.getCountry().setDeleted(true);

        teamPlayerRepository.save(
                new TeamPlayer(team, player, Position.GOALKEEPER, 1)
        );

        // when
        var players = teamPlayerRepository.findAllPlayersByTeamId(team.getId());

        // then
        assertEquals(1, players.size());
        assertNull(players.get(0).getCountryCode());
    }

    @Test
    @DisplayName("findAllPlayersByTeamId native query fetches information from all expected columns")
    public void findAllPlayersByTeamId_NonDeletedTeamAndPlayer_FetchesValuesFromAllExpectedColumns() {
        var team = getTestTeam("Test Team123");
        var player = getTestPlayer("Test Player123");

        var teamPlayer = teamPlayerRepository.save(
                new TeamPlayer(team, player, Position.MIDFIELDER, 50)
        );
        var savedTeamPlayer = teamPlayerRepository.save(teamPlayer);

        // when
        var players = teamPlayerRepository.findAllPlayersByTeamId(team.getId());

        // then
        assertEquals(1, players.size());
        var receivedTeamPlayer = players.get(0);

        assertEquals(savedTeamPlayer.getId(), receivedTeamPlayer.getId());
        assertEquals(savedTeamPlayer.getPosition().toString(), receivedTeamPlayer.getPosition());
        assertEquals(savedTeamPlayer.getNumber(), receivedTeamPlayer.getNumber());
        assertEquals(savedTeamPlayer.getPlayer().getCountry().getCountryCode(), receivedTeamPlayer.getCountryCode());

        var innerPlayer = receivedTeamPlayer.getPlayer();
        assertEquals(player.getId(), innerPlayer.getId());
        assertEquals(player.getName(), innerPlayer.getName());
        assertEquals(player.getDateOfBirth(), innerPlayer.getDateOfBirth());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayerByPlayerId native query finds zero teams when the player does not exist")
    public void findAllTeamsOfPlayerByPlayerId_PlayerDoesNotExist_IsEmpty() {
        var playerId = UUID.randomUUID();

        // when
        var teams = teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(playerId);

        // then
        assertEquals(0, teams.size());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayerByPlayerId native query finds zero teams when the player is marked as deleted")
    public void findAllTeamsOfPlayerByPlayerId_PlayerMarkedAsDeleted_IsEmpty() {
        var team0 = getTestTeam("Some team");
        var team1 = getTestTeam("Some team 123");

        var testPlayer = getTestPlayer("Test1");
        testPlayer.setDeleted(true);

        teamPlayerRepository.save(
                new TeamPlayer(team0, testPlayer, Position.GOALKEEPER, 1)
        );
        teamPlayerRepository.save(
                new TeamPlayer(team1, testPlayer, Position.GOALKEEPER, 1)
        );

        // when
        var teams = teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(testPlayer.getId());

        // then
        assertTrue(teams.isEmpty());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayerByPlayerId native query does not find teams whose assignments to the player are marked as deleted")
    public void findAllTeamsOfPlayerByPlayerId_TeamPlayerMarkedAsDeleted_DoesNotContainTeamWithAssignmentMarkedAsDeleted() {
        var team0 = getTestTeam("Some team");
        var team1 = getTestTeam("Some team 123");

        var testPlayer = getTestPlayer("Test1");
        var deletedTeamPlayer = new TeamPlayer(team0, testPlayer, Position.GOALKEEPER, 1);
        deletedTeamPlayer.setDeleted(true);

        teamPlayerRepository.save(deletedTeamPlayer);
        teamPlayerRepository.save(
                new TeamPlayer(team1, testPlayer, Position.GOALKEEPER, 1)
        );

        // when
        var teams = teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(testPlayer.getId());

        // then
        assertEquals(1, teams.size());
        assertEquals(team1.getName(), teams.get(0).getName());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayerByPlayerId native query finds zero teams when the player's team is marked as deleted")
    public void findAllTeamsOfPlayerByPlayerId_TeamMarkedAsDeleted_IsEmpty() {
        var team = getTestTeam("Some team");
        team.setDeleted(true);

        var testPlayer = getTestPlayer("Test1");
        teamPlayerRepository.save(
                new TeamPlayer(team, testPlayer, Position.GOALKEEPER, 1)
        );

        // when
        var teams = teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(testPlayer.getId());

        // then
        assertTrue(teams.isEmpty());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayerByPlayerId native query does not leak team's country if it's marked as deleted")
    public void findAllTeamsOfPlayerByPlayerId_TeamCountryMarkedAsDeleted_DoesNotLeakDeletedCountry() {
        var team = getTestTeam("Some team");
        team.getCountry().setDeleted(true);

        var testPlayer = getTestPlayer("Test1");
        teamPlayerRepository.save(
                new TeamPlayer(team, testPlayer, Position.GOALKEEPER, 1)
        );

        // when
        var teams = teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(testPlayer.getId());

        // then
        assertEquals(1, teams.size());
        assertNull(teams.get(0).getCountry());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayerByPlayerId native query does not leak team's coach if it's marked as deleted")
    public void findAllTeamsOfPlayerByPlayerId_TeamCoachMarkedAsDeleted_DoesNotLeakDeletedCoach() {
        var team = getTestTeam("Some team");
        team.getCoach().setDeleted(true);

        var testPlayer = getTestPlayer("Test1");
        teamPlayerRepository.save(
                new TeamPlayer(team, testPlayer, Position.GOALKEEPER, 1)
        );

        // when
        var teams = teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(testPlayer.getId());

        // then
        assertEquals(1, teams.size());
        assertNull(teams.get(0).getCoach());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayerByPlayerId native query finds only teams for whom the player plays")
    public void findAllTeamsOfPlayerByPlayerId_MultiplePlayersWithTeams_OnlyContainsExpectedTeamsOfPlayer() {
        var team0 = getTestTeam("Some team");
        var team1 = getTestTeam("Other team");

        var testPlayer = getTestPlayer("Test1");

        teamPlayerRepository.save(
                new TeamPlayer(team0, testPlayer, Position.GOALKEEPER, 1)
        );
        teamPlayerRepository.save(
                new TeamPlayer(team1, getTestPlayer("Some other player"), Position.GOALKEEPER, 1)
        );

        // when
        var teams = teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(testPlayer.getId());

        // then
        assertEquals(1, teams.size());
        assertEquals(team0.getName(), teams.get(0).getName());
    }

    @Test
    @DisplayName("findAllTeamsOfPlayerByPlayerId native query fetches information from all expected columns")
    public void findAllTeamsOfPlayerByPlayerId_NonDeletedTeamAndPlayer_FetchesValuesFromAllExpectedColumns() {
        var team = getTestTeam("Test Team123");
        var player = getTestPlayer("Test Player123");

        var teamPlayer = teamPlayerRepository.save(
                new TeamPlayer(team, player, Position.MIDFIELDER, 50)
        );

        // when
        var teams = teamPlayerRepository.findAllTeamsOfPlayerByPlayerId(player.getId());

        // then
        assertEquals(1, teams.size());
        var receivedTeam = teams.get(0);

        assertEquals(team.getId(), receivedTeam.getId());
        assertEquals(team.getName(), receivedTeam.getName());
        assertEquals(team.getCrestUrl(), receivedTeam.getCrestUrl());
        var coach = team.getCoach();
        assertEquals(coach.getId(), receivedTeam.getCoach().getId());
        assertEquals(coach.getName(), receivedTeam.getCoach().getName());
        var country = team.getCountry();
        assertEquals(country.getId(), receivedTeam.getCountry().getId());
        assertEquals(country.getName(), receivedTeam.getCountry().getName());
        assertEquals(country.getCountryCode(), receivedTeam.getCountry().getCountryCode());
    }

    @Test
    @DisplayName("markTeamPlayerAsDeleted native query only affects the team player with specified id")
    public void markTeamPlayerAsDeleted_SpecifiedTeamPlayerId_OnlyMarksSpecifiedTeamPlayer() {
        var team0 = getTestTeam("Test Team");
        var team1 = getTestTeam("Test Team123");
        var player0 = getTestPlayer("Test Player");
        var player1 = getTestPlayer("Test Player123");

        var teamPlayer0 = teamPlayerRepository.save(
                new TeamPlayer(team0, player0, Position.MIDFIELDER, 50)
        );
        teamPlayerRepository.save(
                new TeamPlayer(team1, player1, Position.MIDFIELDER, 50)
        );

        // when
        var countDeleted = teamPlayerRepository.markTeamPlayerAsDeleted(teamPlayer0.getId());

        // then
        assertEquals(1, countDeleted);
    }

    @Test
    @DisplayName("markTeamPlayerAsDeleted native query only affects not deleted teams")
    public void markTeamPlayerAsDeleted_TeamAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var team0 = getTestTeam("Test Team");
        var team1 = getTestTeam("Test Team123");
        var player0 = getTestPlayer("Test Player");
        var player1 = getTestPlayer("Test Player123");

        var teamPlayer0 = new TeamPlayer(team0, player0, Position.MIDFIELDER, 50);
        teamPlayer0.setDeleted(true);
        teamPlayerRepository.save(teamPlayer0);
        teamPlayerRepository.save(
                new TeamPlayer(team1, player1, Position.MIDFIELDER, 50)
        );

        // when
        var countDeleted = teamPlayerRepository.markTeamPlayerAsDeleted(teamPlayer0.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("teamHasPlayerWithNumber native query returns false when the team does not exist")
    public void teamHasPlayerWithNumber_TeamDoesNotExist_ReturnsFalse() {
        var teamId = UUID.randomUUID();
        var numberToCheck = 1;

        // when
        var numberTaken = teamPlayerRepository.teamHasPlayerWithNumber(teamId, numberToCheck);

        // then
        assertFalse(numberTaken);
    }

    @Test
    @DisplayName("teamHasPlayerWithNumber native query returns false when the team is marked as deleted")
    public void teamHasPlayerWithNumber_TeamMarkedAsDeleted_ReturnsFalse() {
        var team = getTestTeam("Some team");
        team.setDeleted(true);
        var playerNumber = 1;

        var testPlayer = getTestPlayer("Test1");
        teamPlayerRepository.save(
                new TeamPlayer(team, testPlayer, Position.GOALKEEPER, playerNumber)
        );

        // when
        var numberTaken = teamPlayerRepository.teamHasPlayerWithNumber(team.getId(), playerNumber);

        // then
        assertFalse(numberTaken);
    }

    @Test
    @DisplayName("teamHasPlayerWithNumber native query returns false when the player is marked as deleted")
    public void teamHasPlayerWithNumber_PlayerMarkedAsDeleted_ReturnsFalse() {
        var team = getTestTeam("Some team");
        var playerNumber = 1;

        var testPlayer = getTestPlayer("Test1");
        testPlayer.setDeleted(true);
        teamPlayerRepository.save(
                new TeamPlayer(team, testPlayer, Position.GOALKEEPER, playerNumber)
        );

        // when
        var numberTaken = teamPlayerRepository.teamHasPlayerWithNumber(team.getId(), playerNumber);

        // then
        assertFalse(numberTaken);
    }

    @Test
    @DisplayName("teamHasPlayerWithNumber native query returns false when the number is not taken")
    public void teamHasPlayerWithNumber_NumberNotTaken_ReturnsFalse() {
        var team = getTestTeam("Some team");
        var playerNumber = 1;
        var otherNumber = 2;

        var testPlayer = getTestPlayer("Test1");
        teamPlayerRepository.save(
                new TeamPlayer(team, testPlayer, Position.GOALKEEPER, playerNumber)
        );

        // when
        var numberTaken = teamPlayerRepository.teamHasPlayerWithNumber(team.getId(), otherNumber);

        // then
        assertFalse(numberTaken);
    }

    @Test
    @DisplayName("teamHasPlayerWithNumber native query returns true when the number is taken")
    public void teamHasPlayerWithNumber_NumberTaken_ReturnsTrue() {
        var team = getTestTeam("Some team");
        var playerNumber = 1;

        var testPlayer = getTestPlayer("Test1");
        teamPlayerRepository.save(
                new TeamPlayer(team, testPlayer, Position.GOALKEEPER, playerNumber)
        );

        // when
        var numberTaken = teamPlayerRepository.teamHasPlayerWithNumber(team.getId(), playerNumber);

        // then
        assertTrue(numberTaken);
    }
}
