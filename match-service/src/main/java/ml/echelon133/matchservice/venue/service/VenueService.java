package ml.echelon133.matchservice.venue.service;

import ml.echelon133.common.exception.ResourceNotFoundException;
import ml.echelon133.common.venue.dto.VenueDto;
import ml.echelon133.matchservice.venue.model.UpsertVenueDto;
import ml.echelon133.matchservice.venue.model.Venue;
import ml.echelon133.matchservice.venue.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    @Autowired
    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    private static VenueDto entityToDto(Venue venue) {
        return new VenueDto(venue.getId(), venue.getName(), venue.getCapacity());
    }

    /**
     * Returns the information about the venue with specified id.
     *
     * @param id id of the venue
     * @return a dto representing the venue
     * @throws ResourceNotFoundException thrown when the venue does not exist in the database
     */
    public VenueDto findById(UUID id) throws ResourceNotFoundException {
        return this.venueRepository
                .findById(id)
                .map(VenueService::entityToDto)
                .orElseThrow(() -> new ResourceNotFoundException(Venue.class, id));
    }

    /**
     * Updates the venue's information.
     *
     * @param id id of the venue to update
     * @param venueDto dto containing values to be placed in the database
     * @return a dto representing the updated venue
     * @throws ResourceNotFoundException thrown when the venue does not exist in the database
     */
    public VenueDto updateVenue(UUID id, UpsertVenueDto venueDto) throws ResourceNotFoundException {
        return null;
    }

    /**
     * Creates the venue's entry in the database.
     *
     * @param venueDto dto representing the information about a venue that will be saved in the database
     * @return a dto representing the newly saved venue
     */
    public VenueDto createVenue(UpsertVenueDto venueDto) {
        return null;
    }

    /**
     * Finds all venues whose names contain a specified phrase.
     *
     * @param phrase phrase which needs to appear in the name of the venue
     * @return a page of venues which match the filter
     */
    public Page<VenueDto> findVenuesByName(String phrase) {
        return null;
    }

    /**
     * Deletes a venue with the specified id.
     *
     * @param id id of the venue to be deleted
     * @throws ResourceNotFoundException thrown when the venue does not exist in the database at all
     */
    public void deleteVenue(UUID id) throws ResourceNotFoundException {
        return;
    }
}
