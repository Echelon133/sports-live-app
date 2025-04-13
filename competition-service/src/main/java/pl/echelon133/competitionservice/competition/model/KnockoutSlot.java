package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.*;
import ml.echelon133.common.entity.BaseEntity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderColumn(name = "legs_order")
        private List<CompetitionMatch> legs;

        public Taken() {
            super(SlotType.TAKEN);
        }

        public Taken(CompetitionMatch firstLeg, CompetitionMatch secondLeg) {
            this();
            this.legs = Stream.of(firstLeg, secondLeg).filter(Objects::nonNull).collect(Collectors.toList());
        }
        public Taken(CompetitionMatch firstLeg) {
            this(firstLeg, null);
        }

        public List<CompetitionMatch> getLegs() {
            return legs;
        }

        public void setLegs(List<CompetitionMatch> legs) {
            this.legs = legs;
        }
    }
}