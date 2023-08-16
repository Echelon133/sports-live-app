package ml.echelon133.common.venue.dto;

import java.util.UUID;

public class VenueDto {
    private UUID id;
    private String name;
    private Integer capacity;

    public VenueDto() {}
    public VenueDto(UUID id, String name, Integer capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
