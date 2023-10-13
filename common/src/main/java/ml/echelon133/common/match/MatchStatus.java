package ml.echelon133.common.match;

import java.util.Arrays;
import java.util.List;
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
}
