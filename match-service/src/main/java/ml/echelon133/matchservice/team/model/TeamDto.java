package ml.echelon133.matchservice.team.model;

import ml.echelon133.matchservice.coach.model.CoachDto;
import ml.echelon133.matchservice.country.model.CountryDto;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public interface TeamDto {
    UUID getId();
    String getName();
    String getCrestUrl();

    // if country is deleted, set this value to null to prevent any leakage of data (seems to be the simplest solution while using native queries)
    @Value("#{target.countryDeleted ? null : (T(ml.echelon133.matchservice.country.model.CountryDto).from(target.countryId, target.countryName, target.countryCode))}")
    CountryDto getCountry();

    // if coach is deleted, set this value to null to prevent any leakage of data (seems to be the simplest solution while using native queries)
    @Value("#{target.coachDeleted ? null : (T(ml.echelon133.matchservice.coach.model.CoachDto).from(target.coachId, target.coachName))}")
    CoachDto getCoach();

    static TeamDto from(UUID id, String name, String crestUrl, CountryDto countryDto, CoachDto coachDto) {
        return new TeamDto() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getCrestUrl() {
                return crestUrl;
            }

            @Override
            public CountryDto getCountry() {
                return countryDto;
            }

            @Override
            public CoachDto getCoach() {
                return coachDto;
            }
        };
    }
}
