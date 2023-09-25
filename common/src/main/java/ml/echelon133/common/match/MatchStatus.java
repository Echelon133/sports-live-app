package ml.echelon133.common.match;

/**
 * All possible statuses of a match.
 */
public enum MatchStatus {
    NOT_STARTED,
    FIRST_HALF,
    HALF_TIME,
    SECOND_HALF,
    FINISHED,
    EXTRA_TIME,
    PENALTIES,
    POSTPONED,
    ABANDONED
}
