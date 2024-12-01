package ml.echelon133.common.event.dto;

import java.io.Serializable;

public record SerializedScore(int homeGoals, int awayGoals) implements Serializable {
}
