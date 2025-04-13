package pl.echelon133.competitionservice.competition.model;

import jakarta.persistence.*;
import ml.echelon133.common.entity.BaseEntity;

import java.util.List;

@Entity
public class Stage extends BaseEntity {

    @Column(columnDefinition = "VARCHAR(25)")
    @Enumerated(value = EnumType.STRING)
    private KnockoutStage stage;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "slot_order")
    private List<KnockoutSlot> slots;

    public Stage() {}
    public Stage(KnockoutStage stage) {
        this.stage = stage;
    }

    public KnockoutStage getStage() {
        return stage;
    }

    public void setStage(KnockoutStage stage) {
        this.stage = stage;
    }

    public List<KnockoutSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<KnockoutSlot> slots) {
        this.slots = slots;
    }
}
