package ml.echelon133.common.event.dto;

import java.util.UUID;

public interface MatchEventDto {
    UUID getId();
    MatchEventDetails getEvent();

    static MatchEventDto from(UUID id, MatchEventDetails eventDto) {
        return new MatchEventDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public MatchEventDetails getEvent() {
                return eventDto;
            }
        };
    }
}
