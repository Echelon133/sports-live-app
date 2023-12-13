package ml.echelon133.common.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ml.echelon133.common.event.MatchEventType;
import ml.echelon133.common.match.MatchStatus;

import java.io.Serializable;
import java.util.UUID;

/**
 * Data class which encapsulates detailed information about a match event that has already been processed.
 *
 * There are multiple types of events, each type has a different number of fields of various types.
 * Serialization of this class lets us easily store these complex events in a single database column.
 */
public abstract class MatchEventDetails implements Serializable {

    // this field contains information about the JSON subtype of the serialized object --
    // @JsonIgnore prevents this field from being included twice in the output string
    @JsonIgnore
    private MatchEventType type;
    private String minute;
    private UUID competitionId;

    public MatchEventDetails() {}
    public MatchEventDetails(MatchEventType type, String minute, UUID competitionId) {
        this.type = type;
        this.minute = minute;
        this.competitionId = competitionId;
    }

    public MatchEventType getType() {
        return type;
    }

    public void setType(MatchEventType type) {
        this.type = type;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public UUID getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(UUID competitionId) {
        this.competitionId = competitionId;
    }

    /**
     * Data class representing already processed match event of type <b>STATUS</b>.
     */
    public static class StatusDto extends MatchEventDetails {
        private MatchStatus targetStatus;

        public StatusDto() {}
        public StatusDto(String minute, UUID competitionId, MatchStatus targetStatus) {
            super(MatchEventType.STATUS, minute, competitionId);
            this.targetStatus = targetStatus;
        }

        public MatchStatus getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(MatchStatus targetStatus) {
            this.targetStatus = targetStatus;
        }
    }
}
