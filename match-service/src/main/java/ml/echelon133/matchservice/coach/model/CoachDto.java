package ml.echelon133.matchservice.coach.model;

import java.util.UUID;

public interface CoachDto {
    UUID getId();
    String getName();

    static CoachDto from(UUID id, String name) {
        return new CoachDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
