package ml.echelon133.matchservice.coach.model;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

public class UpsertCoachDto {

    @NotNull
    @Length(min = 1, max = 100)
    private String name;

    public UpsertCoachDto() {}
    public UpsertCoachDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
