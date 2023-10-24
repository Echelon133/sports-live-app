package ml.echelon133.matchservice.venue.model;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class UpsertVenueDto {

    @NotNull
    @Length(min = 1, max = 120)
    private String name;

    @Min(value = 0, message = "expected positive integers")
    private Integer capacity;

    public UpsertVenueDto() {}
    public UpsertVenueDto(String name, Integer capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
