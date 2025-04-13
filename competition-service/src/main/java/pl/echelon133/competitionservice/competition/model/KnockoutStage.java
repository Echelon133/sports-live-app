package pl.echelon133.competitionservice.competition.model;

import java.util.Arrays;
import java.util.List;

/**
 * All possible types of stages in a knockout competition.
 */
public enum KnockoutStage {
    ROUND_OF_128(64),
    ROUND_OF_64(32),
    ROUND_OF_32(16),
    ROUND_OF_16(8),
    QUARTER_FINAL(4),
    SEMI_FINAL(2),
    FINAL(1);

    private final int slots;
    KnockoutStage(int slots) {
        this.slots = slots;
    }

    /**
     * Returns the number of match slots in this stage.
     * <p>The number of match slots is <b>NOT</b> equal to the number
     * of matches happening at a particular stage. If a particular stage has two
     * legs to determine the winner, both legs are contained within a single
     * slot.
     * </p>
     * @return number of match slots at this stage
     */
    public int getSlots() {
        return slots;
    }

    /**
     * Returns a list of knockout stages which occur after the given stage (including
     * that stage).
     *
     * <p>For example, given <b>QUARTER_FINAL</b>, this method will return
     * <ul>
     *     <li>QUARTER_FINAL</li>
     *     <li>SEMI_FINAL</li>
     *     <li>FINAL</li>
     * </ul>
     * </p>
     *
     * @param stage stage at which the competition starts
     * @return stages which must be included in a competition which starts at the given stage
     */
    public static List<KnockoutStage> getStagesForCompetition(KnockoutStage stage) {
        return Arrays.stream(KnockoutStage.values()).dropWhile(p -> !p.equals(stage)).toList();
    }

    /**
     * Returns the enum constant of this type with the specified name (case-insensitive).
     *
     * @param stage case-insensitive string representing the enum constant
     * @return the enum constant with the specified name (case-insensitive)
     * @throws IllegalArgumentException thrown when there is no constant associated with that name
     */
    public static KnockoutStage valueOfIgnoreCase(String stage) {
        return KnockoutStage.valueOf(stage.toUpperCase());
    }
}
