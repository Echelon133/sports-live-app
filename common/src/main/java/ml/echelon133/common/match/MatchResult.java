package ml.echelon133.common.match;

import java.io.Serializable;

/**
 * Possible results of a match.
 */
public enum MatchResult implements Serializable {
    NONE,
    HOME_WIN,
    AWAY_WIN,
    DRAW;

    /**
     * Returns the result depending on the score in the game.
     * @param homeGoals the number of goals scored by the home side
     * @param awayGoals the number of goals scored by the away side
     * @return enum value which states the match result
     */
    public static MatchResult getResultBasedOnScore(int homeGoals, int awayGoals) {
        MatchResult outputResult;
        if (homeGoals > awayGoals) {
            outputResult = HOME_WIN;
        } else if (homeGoals < awayGoals) {
            outputResult = AWAY_WIN;
        } else {
            outputResult = DRAW;
        }
        return outputResult;
    }
}
