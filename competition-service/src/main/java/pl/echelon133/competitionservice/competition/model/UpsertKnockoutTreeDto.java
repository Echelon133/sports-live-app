package pl.echelon133.competitionservice.competition.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.echelon133.competitionservice.competition.model.constraints.KnockoutStageValue;
import pl.echelon133.competitionservice.competition.model.constraints.MatchesUnique;
import pl.echelon133.competitionservice.competition.model.constraints.StageNamesUnique;
import pl.echelon133.competitionservice.competition.model.constraints.StageSlotSizeExact;

import java.util.List;
import java.util.UUID;

@StageNamesUnique
@MatchesUnique
public record UpsertKnockoutTreeDto(
        // KnockoutStage enum has 7 values, therefore there is at most 7 types of knockout stages
        @NotNull @Size(min = 1, max = 7, message = "expected between {min} and {max} stages") List<@Valid UpsertStage> stages
) {

    @StageSlotSizeExact
    public record UpsertStage(
            @NotNull @KnockoutStageValue String stage,
            @NotNull List<@Valid UpsertKnockoutSlot> slots
    ) {}

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            visible = true,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Empty.class, name = "EMPTY"),
            @JsonSubTypes.Type(value = Bye.class, name = "BYE"),
            @JsonSubTypes.Type(value = Taken.class, name = "TAKEN"),
    })
    // Ignoring "type" property during deserialization is required to avoid "UnrecognizedPropertyException"
    // caused by the deserializer trying to insert "type" property into the record, without the "knowledge" that
    // this property is not stored as a property on a record, and instead is only a constant accessible via
    // an accessor method.
    @JsonIgnoreProperties(value = "type")
    public sealed interface UpsertKnockoutSlot permits Empty, Bye, Taken {
        String type();
    }

    public record Empty() implements UpsertKnockoutSlot {
        @Override
        public String type() {
            return KnockoutSlot.SlotType.EMPTY.name();
        }
    }

    public record Bye(@NotNull UUID teamId) implements UpsertKnockoutSlot {
        @Override
        public String type() {
            return KnockoutSlot.SlotType.BYE.name();
        }
    }

    public record Taken(@NotNull UUID firstLeg, UUID secondLeg) implements UpsertKnockoutSlot {
        @Override
        public String type() {
            return KnockoutSlot.SlotType.TAKEN.name();
        }
    }
}
