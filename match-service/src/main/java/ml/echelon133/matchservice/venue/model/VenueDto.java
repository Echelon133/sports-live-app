package ml.echelon133.matchservice.venue.model;

import java.util.UUID;

public interface VenueDto {
    UUID getId();
    String getName();
    Integer getCapacity();

    static VenueDto from(UUID id, String name, Integer capacity) {
        return new VenueDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Integer getCapacity() {
                return capacity;
            }
        };
    }
}
