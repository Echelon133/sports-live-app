package ml.echelon133.matchservice.referee.model;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotNull;

public class UpsertRefereeDto {

    @NotNull
    @Length(min = 1, max = 100)
    private String name;

    public UpsertRefereeDto() {}
    public UpsertRefereeDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
