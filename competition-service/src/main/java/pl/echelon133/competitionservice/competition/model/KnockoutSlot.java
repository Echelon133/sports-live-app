package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.*;
import ml.echelon133.common.entity.BaseEntity;

import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class KnockoutSlot extends BaseEntity {

    public enum SlotType {
        // Slot does not contain any matches
        EMPTY,
        // Slot contains information about a team skipping a stage without having to play a match
        BYE,
        // Slot contains information about the first and (optionally) second leg of the match-up
        TAKEN
    }

    @Enumerated(EnumType.STRING)
    private SlotType type;

    public KnockoutSlot() {}
    public KnockoutSlot(SlotType type) {
        this.type = type;
    }

    public SlotType getType() {
        return type;
    }

    public void setType(SlotType type) {
        this.type = type;
    }

    @Entity
    @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
    public static class Empty extends KnockoutSlot {
        public Empty() {
            super(SlotType.EMPTY);
        }
    }

    @Entity
    @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
    public static class Bye extends KnockoutSlot {
        private UUID teamId;

        public Bye() {
            super(SlotType.BYE);
        }
        public Bye(UUID teamId) {
            this();
            this.teamId = teamId;
        }

        public UUID getTeamId() {
            return teamId;
        }

        public void setTeamId(UUID teamId) {
            this.teamId = teamId;
        }
    }

    @Entity
    @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
    public static class Taken extends KnockoutSlot {
        private UUID firstLeg;
        private UUID secondLeg;

        public Taken() {
            super(SlotType.TAKEN);
        }
        public Taken(UUID firstLeg) {
            this();
            this.firstLeg = firstLeg;
        }
        public Taken(UUID firstLeg, UUID secondLeg) {
            this(firstLeg);
            this.secondLeg = secondLeg;
        }

        public UUID getFirstLeg() {
            return firstLeg;
        }

        public void setFirstLeg(UUID firstLeg) {
            this.firstLeg = firstLeg;
        }

        public UUID getSecondLeg() {
            return secondLeg;
        }

        public void setSecondLeg(UUID secondLeg) {
            this.secondLeg = secondLeg;
        }
    }
}
