package ml.echelon133.common.event;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Types of match events.
 *
 * <ul>
 *     <li>STATUS - the change of the match status - i.e. match begins, half-time begins, match ends</li>
 *     <li>GOAL - either team scores a goal</li>
 *     <li>CARD - some player receives a yellow/red card</li>
 *     <li>SUBSTITUTION - some player is substituted</li>
 *     <li>COMMENTARY - a context to something happening on the pitch is provided</li>
 *     <li>PENALTY - a penalty kick (either during the match or the penalty shootout)</li>
 * </ul>
 */
public enum MatchEventType {
    STATUS,
    GOAL,
    CARD,
    SUBSTITUTION,
    COMMENTARY,
    PENALTY;

    /**
     * All match event types (represented as a list of strings).
     */
    public final static List<String> ALL_EVENT_TYPES =
            Arrays.stream(MatchEventType.values()).map(Enum::name).collect(Collectors.toList());

    /**
     * Returns the enum constant of this type with the specified name (case-insensitive).
     *
     * @param type case-insensitive string representing the enum constant
     * @return the enum constant with the specified name (case-insensitive)
     */
    public static MatchEventType valueOfIgnoreCase(String type) {
        return MatchEventType.valueOf(type.toUpperCase());
    }
}
