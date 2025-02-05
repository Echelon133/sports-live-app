package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.*;
import ml.echelon133.common.entity.BaseEntity;

import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
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
    public static class Empty extends KnockoutSlot {
        public Empty() {
            super(SlotType.EMPTY);
        }
    }

    @Entity
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
    public static class Taken extends KnockoutSlot {

        @AttributeOverrides({
                @AttributeOverride(name = "matchId", column = @Column(name = "first_leg_match_id")),
                @AttributeOverride(name = "finished", column = @Column(name = "first_leg_finished")),
        })
        @Embedded
        private CompetitionMatch firstLeg;

        @AttributeOverrides({
                @AttributeOverride(name = "matchId", column = @Column(name = "second_leg_match_id")),
                @AttributeOverride(name = "finished", column = @Column(name = "second_leg_finished")),
        })
        @Embedded
        private CompetitionMatch secondLeg;

        public Taken() {
            super(SlotType.TAKEN);
        }

        public Taken(CompetitionMatch firstLeg) {
            this();
            this.firstLeg = firstLeg;
        }
        public Taken(CompetitionMatch firstLeg, CompetitionMatch secondLeg) {
            this(firstLeg);
            this.secondLeg = secondLeg;
        }

        public CompetitionMatch getFirstLeg() {
            return firstLeg;
        }

        public void setFirstLeg(CompetitionMatch firstLeg) {
            this.firstLeg = firstLeg;
        }

        public CompetitionMatch getSecondLeg() {
            return secondLeg;
        }

        public void setSecondLeg(CompetitionMatch secondLeg) {
            this.secondLeg = secondLeg;
        }
    }
}