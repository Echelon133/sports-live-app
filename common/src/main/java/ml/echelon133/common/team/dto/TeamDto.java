package ml.echelon133.common.team.dto;

import ml.echelon133.common.coach.dto.CoachDto;
import ml.echelon133.common.country.dto.CountryDto;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public interface TeamDto {
    UUID getId();
    String getName();

    // if country is deleted, set this value to null to prevent any leakage of data (seems to be the simplest solution while using native queries)
    @Value("#{target.countryDeleted ? null : (T(ml.echelon133.common.country.dto.CountryDto).from(target.countryId, target.countryName, target.countryCode))}")
    CountryDto getCountry();

    // if coach is deleted, set this value to null to prevent any leakage of data (seems to be the simplest solution while using native queries)
    @Value("#{target.coachDeleted ? null : (T(ml.echelon133.common.coach.dto.CoachDto).from(target.coachId, target.coachName))}")
    CoachDto getCoach();

    static TeamDto from(UUID id, String name, CountryDto countryDto, CoachDto coachDto) {
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
