package ml.echelon133.matchservice.country.repository;

import ml.echelon133.common.country.dto.CountryDto;
import ml.echelon133.matchservice.country.model.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, UUID> {

    /**
     * Finds a non-deleted country with the specified id.
     *
     * @param id id of the country
     * @return empty {@link Optional} if the country was not found or is marked as deleted, otherwise contains a {@link CountryDto}
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(value = "SELECT CAST(id as varchar) as id, name, country_code as countryCode FROM country WHERE deleted = false AND id = ?1",
            nativeQuery = true)
    Optional<CountryDto> findCountryById(UUID id);

    /**
     * Marks the country with the specified id as deleted.
     *
     * @param id id of the country to be marked as deleted
     * @return count of how many countries had been marked as deleted by this query
     */
    @Modifying
    @Query(value = "UPDATE country SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    Integer markCountryAsDeleted(UUID id);

    /**
     * Finds all countries which contain a certain phrase in their name.
     *
     * @param phrase phrase (case-insensitive) which has to appear in the name
     * @param pageable information about the wanted page
     * @return a page containing all countries whose names contain the phrase
     */
    // CAST(id as varchar) is a workaround for https://github.com/spring-projects/spring-data-jpa/issues/1796
    @Query(value = "SELECT CAST(id as varchar) as id, name, country_code as countryCode FROM country WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            countQuery = "SELECT COUNT(*) FROM country WHERE LOWER(name) LIKE '%' || LOWER(:phrase) || '%' AND deleted = false",
            nativeQuery = true)
    Page<CountryDto> findAllByNameContaining(String phrase, Pageable pageable);
}
