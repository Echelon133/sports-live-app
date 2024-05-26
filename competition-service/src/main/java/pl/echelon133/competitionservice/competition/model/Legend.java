package pl.echelon133.competitionservice.competition.model;

import ml.echelon133.common.entity.BaseEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Set;

@Entity
public class Legend extends BaseEntity {

    @ElementCollection
    private Set<Integer> positions;

    private String context;

    @Enumerated(value = EnumType.STRING)
    private LegendSentiment sentiment;

    public Legend() {}
    public Legend(Set<Integer> positions, String context, LegendSentiment sentiment) {
        this.positions = positions;
        this.context = context;
        this.sentiment = sentiment;
    }

    public Set<Integer> getPositions() {
        return positions;
    }

    public void setPositions(Set<Integer> positions) {
        this.positions = positions;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public LegendSentiment getSentiment() {
        return sentiment;
    }

    public void setSentiment(LegendSentiment sentiment) {
        this.sentiment = sentiment;
    }

    /**
     * Contains information about the sentiment of a particular set of positions in a competition.
     *
     * If an example competition offers:
     * <ul>
     *     <li>promotion to the Champions League to positions 1, 2, 3, 4</li>
     *     <li>promotion to the Europa League to position 5</li>
     *     <li>promotion to the Conference League to position 6</li>
     *     <li>relegation to a lower league to positions 18, 19, 20</li>
     * </ul>
     *
     * then we would want to color the first three types of promotion using colors which carry a positive sentiment,
     * while the last one with a color that carries a negative sentiment (e.g. bright red).
     * Storing only the information about the sentiment lets us pick actual colors client-side.
     */
    public enum LegendSentiment {
        POSITIVE_A,
        POSITIVE_B,
        POSITIVE_C,
        POSITIVE_D,
        NEGATIVE_A,
        NEGATIVE_B;

        /**
         * Returns the enum constant of this type with the specified name (case-insensitive).
         *
         * @param sentiment case-insensitive string representing the enum constant
         * @return the enum constant with the specified name (case-insensitive)
         * @throws IllegalArgumentException thrown when there is no constant associated with that name
         */
        public static LegendSentiment valueOfCaseIgnore(String sentiment) {
            return LegendSentiment.valueOf(sentiment.toUpperCase());
        }
    }
}
