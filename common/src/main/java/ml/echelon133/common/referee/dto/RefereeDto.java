package ml.echelon133.common.referee.dto;

import java.util.UUID;

public interface RefereeDto {
    UUID getId();
    String getName();

    static RefereeDto from(UUID id, String name) {
        return new RefereeDto() {
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
