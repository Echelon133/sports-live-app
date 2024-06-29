package ml.echelon133.common.event;

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
}
