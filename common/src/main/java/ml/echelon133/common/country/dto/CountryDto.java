package ml.echelon133.common.country.dto;

import java.util.UUID;

public interface CountryDto {
    UUID getId();
    String getName();
    String getCountryCode();

    static CountryDto from(UUID id, String name, String countryCode) {
        return new CountryDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getCountryCode() {
                return countryCode;
            }
        };
    }
}
