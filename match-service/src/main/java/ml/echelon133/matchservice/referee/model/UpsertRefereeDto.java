package ml.echelon133.matchservice.referee.model;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

public class UpsertRefereeDto {

    @NotNull(message = "field has to be provided")
    @Length(min = 1, max = 100, message = "expected length between 1 and 100")
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
