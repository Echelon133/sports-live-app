package ml.echelon133.matchservice.match;

import ml.echelon133.matchservice.match.model.Lineup;
import ml.echelon133.matchservice.match.model.Match;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;
import ml.echelon133.matchservice.team.TestTeam;
import ml.echelon133.matchservice.team.model.TeamPlayer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestMatchLineup {

    /**
     * Creates a test match with randomly generated lineups that always consist of:
     * <ul>
     *     <li>11 starting players for each team</li>
     *     <li>6 substitution players for each team</li>
     * </ul>
     *
     * @return a match with
     */
    public static Match createTestMatchWithLineup() {
        var testCountry = "PL";
        var testPosition = Position.DEFENDER;

        Match testMatch = TestMatch.builder()
                .homeTeam(TestTeam.builder())
                .awayTeam(TestTeam.builder())
                .build();

        // create 11 starting players for both teams
        var targetStartingSize = 11;
        List<TeamPlayer> homeStartingPlayers = new ArrayList<>(targetStartingSize);
        List<TeamPlayer> awayStartingPlayers = new ArrayList<>(targetStartingSize);
        for (int playerIndex = 0; playerIndex < targetStartingSize; playerIndex++) {
            var playerName = String.valueOf(playerIndex);

            var homePlayer = new Player(playerName, testPosition, LocalDate.of(1970, 1, 1), testCountry);
            var homeTeamPlayer = new TeamPlayer(testMatch.getHomeTeam(), homePlayer, testPosition, playerIndex);
            homeStartingPlayers.add(homeTeamPlayer);

            var awayPlayer = new Player(playerName, testPosition, LocalDate.of(1970, 1, 1), testCountry);
            var awayTeamPlayer = new TeamPlayer(testMatch.getAwayTeam(), awayPlayer, testPosition, playerIndex);
            awayStartingPlayers.add(awayTeamPlayer);
        }

        // create 6 substitute players for both teams
        var targetSubstituteSize = 6;
        List<TeamPlayer> homeSubstitutePlayers = new ArrayList<>(targetSubstituteSize);
        List<TeamPlayer> awaySubstitutePlayers = new ArrayList<>(targetSubstituteSize);
        for (int playerIndex = 0; playerIndex < targetSubstituteSize; playerIndex++) {
            var playerName = String.valueOf(playerIndex);

            var homePlayer = new Player(playerName, testPosition, LocalDate.of(1970, 1, 1), testCountry);
            var homeTeamPlayer = new TeamPlayer(testMatch.getHomeTeam(), homePlayer, testPosition, playerIndex);
            homeSubstitutePlayers.add(homeTeamPlayer);

            var awayPlayer = new Player(playerName, testPosition, LocalDate.of(1970, 1, 1), testCountry);
            var awayTeamPlayer = new TeamPlayer(testMatch.getAwayTeam(), awayPlayer, testPosition, playerIndex);
            awaySubstitutePlayers.add(awayTeamPlayer);
        }

        Lineup homeLineup = testMatch.getHomeLineup();
        Lineup awayLineup = testMatch.getAwayLineup();
        homeLineup.setStartingPlayers(homeStartingPlayers);
        homeLineup.setSubstitutePlayers(homeSubstitutePlayers);
        awayLineup.setStartingPlayers(awayStartingPlayers);
        awayLineup.setSubstitutePlayers(awaySubstitutePlayers);
        return testMatch;
    }
}
