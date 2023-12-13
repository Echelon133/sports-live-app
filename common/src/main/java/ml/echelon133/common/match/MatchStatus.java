package ml.echelon133.common.match;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    ABANDONED;


    /**
     * All valid possibilities of match's status change.
     */
    public static final Map<MatchStatus, List<MatchStatus>> VALID_STATUS_CHANGES = Map.of(
            NOT_STARTED, List.of(FIRST_HALF, ABANDONED, POSTPONED),
            FIRST_HALF, List.of(HALF_TIME, ABANDONED),
            HALF_TIME, List.of(SECOND_HALF, ABANDONED),
            SECOND_HALF, List.of(FINISHED, EXTRA_TIME, ABANDONED),
            EXTRA_TIME, List.of(FINISHED, PENALTIES, ABANDONED),
            PENALTIES, List.of(FINISHED, ABANDONED),
            POSTPONED, List.of(),
            ABANDONED, List.of()
    );

    /**
     * All match statuses (represented as a list of strings).
     */
    public final static List<String> ALL_STATUSES =
            Arrays.stream(MatchStatus.values()).map(Enum::name).collect(Collectors.toList());

    /**
     * All match statuses which signify that a match is finished (represented as a list of strings).
     */
    public final static List<String> RESULT_TYPE_STATUSES =
            Stream.of(FINISHED, ABANDONED).map(Enum::name).collect(Collectors.toList());

    /**
     * All match statuses which signify that a match is not finished (represented as a list of strings).
     */
    public final static List<String> FIXTURE_TYPE_STATUSES =
            Stream.of(
                    NOT_STARTED, FIRST_HALF, HALF_TIME,
                    SECOND_HALF, EXTRA_TIME, PENALTIES, POSTPONED
            ).map(Enum::name).collect(Collectors.toList());

    /**
     * Returns the enum constant of this type with the specified name (case-insensitive).
     *
     * @param status case-insensitive string representing the enum constant
     * @return the enum constant with the specified name (case-insensitive)
     * @throws IllegalArgumentException thrown when there is no constant associated with that name
     */
    public static MatchStatus valueOfCaseIgnore(String status) {
        return MatchStatus.valueOf(status.toUpperCase());
    }

    /**
     * Returns `true` if the status signifies that the ball in the match is in play.
     * @return `true` if the ball is in play, otherwise `false`
     */
    public boolean isBallInPlay() {
        switch (this) {
            case FIRST_HALF:
            case SECOND_HALF:
            case EXTRA_TIME:
            case PENALTIES:
                return true;
            default:
                return false;
        }
    }
}
