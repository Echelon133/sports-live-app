package pl.echelon133.competitionservice.competition.model;

import java.util.List;

public record KnockoutPhaseDto(List<StageDto> stages) {

    public record StageDto(String stage, List<KnockoutSlotDto> slots) {}

    public sealed interface KnockoutSlotDto permits EmptySlotDto, ByeSlotDto, TakenSlotDto {
        String getType();
    }

    public record EmptySlotDto() implements KnockoutSlotDto {
        @Override
        public String getType() {
            return KnockoutSlot.SlotType.EMPTY.name();
        }
    }

    public record ByeSlotDto(TeamDetailsDto team) implements KnockoutSlotDto {
        @Override
        public String getType() {
            return KnockoutSlot.SlotType.BYE.name();
        }
    }

    public record TakenSlotDto(CompactMatchDto firstLeg, CompactMatchDto secondLeg) implements KnockoutSlotDto {
        @Override
        public String getType() {
            return KnockoutSlot.SlotType.TAKEN.name();
        }
    }
}
