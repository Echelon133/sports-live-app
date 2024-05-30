package ml.echelon133.matchservice.player.model;

import ml.echelon133.matchservice.country.model.CountryDto;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.UUID;

public interface PlayerDto {
    UUID getId();
    String getName();
    String getPosition();
    LocalDate getDateOfBirth();

    // if country is deleted, set this value to null to prevent any leakage of data (seems to be the simplest solution while using native queries)
    @Value("#{target.countryDeleted ? null : (T(ml.echelon133.matchservice.country.model.CountryDto).from(target.countryId, target.countryName, target.countryCode))}")
    CountryDto getCountry();

    static PlayerDto from(UUID id, String name, String position, LocalDate dateOfBirth, CountryDto countryDto) {
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
            public CountryDto getCountry() {
                return countryDto;
            }
        };
    }
}
