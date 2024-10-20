package ml.echelon133.matchservice.player.model;

import java.time.LocalDate;
import java.util.UUID;

public interface PlayerDto {
    UUID getId();
    String getName();
    String getPosition();
    LocalDate getDateOfBirth();
    String getCountryCode();

    static PlayerDto from(UUID id, String name, String position, LocalDate dateOfBirth, String countryCode) {
        return new PlayerDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getPosition() {
                return position;
            }

            @Override
            public LocalDate getDateOfBirth() {
                return dateOfBirth;
            }

            @Override
            public String getCountryCode() {
                return countryCode;
            }
        };
    }
}
