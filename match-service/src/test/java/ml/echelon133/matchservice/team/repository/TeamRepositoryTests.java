package ml.echelon133.matchservice.team.repository;

import ml.echelon133.common.team.dto.TeamDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.team.model.Team;
import ml.echelon133.matchservice.team.service.TeamMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class TeamRepositoryTests {

    private final TeamRepository teamRepository;

    @Autowired
    public TeamRepositoryTests(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    private static Team getTestTeam() {
        return new Team(
                "Test team",
                new Country("Poland", "PL")
        );
    }

    private static Team getTestTeamWithName(String name) {
        return new Team(
                name,
                new Country("Poland", "PL")
        );
    }

    // Compare two entities only by the values from columns that are being fetched by our custom database queries
    private static boolean entitiesEqual(Team e1, Team e2) {
        return e1.getId().equals(e2.getId()) &&
                e1.getName().equals(e2.getName()) &&
                e1.getCountry().getId().equals(e2.getCountry().getId()) &&
                e1.getCountry().getName().equals(e2.getCountry().getName()) &&
                e1.getCountry().getCountryCode().equals(e2.getCountry().getCountryCode());
    }

    private static void assertEntityAndDtoEqual(Team teamEntity, TeamDto teamDto) {
        assertTrue(entitiesEqual(TeamMapper.dtoToEntity(teamDto), teamEntity));
    }

    @Test
    @DisplayName("findTeamById native query finds empty when the team does not exist")
    public void findTeamById_TeamDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        Optional<TeamDto> teamDto = teamRepository.findTeamById(id);

        // then
        assertTrue(teamDto.isEmpty());
    }

    @Test
    @DisplayName("findTeamById native query finds team when the team exists")
    public void findTeamById_TeamExists_IsPresent() {
        var team = teamRepository.save(getTestTeam());
        var saved = teamRepository.save(team);

        // when
        var teamDto = teamRepository.findTeamById(saved.getId());

        // then
        assertTrue(teamDto.isPresent());
        assertEntityAndDtoEqual(team, teamDto.get());
    }

    @Test
    @DisplayName("findTeamById native query finds team when the team exists and does not leak deleted country")
    public void findTeamById_TeamExistsAndCountryDeleted_IsPresentAndDoesNotLeakDeletedCountry() {
        var country = new Country("Poland", "PL");
        country.setDeleted(true);
        var team = getTestTeam();
        team.setCountry(country);
        var savedTeam = teamRepository.save(team);

        // when
        var teamDto = teamRepository.findTeamById(savedTeam.getId());

        // then
        assertTrue(teamDto.isPresent());
        assertNull(teamDto.get().getCountry());
    }

    @Test
    @DisplayName("findTeamById native query does not fetch teams marked as deleted")
    public void findTeamById_TeamMarkedAsDeleted_IsEmpty() {
        var teamToDelete = getTestTeam();
        teamToDelete.setDeleted(true);
        var saved = teamRepository.save(teamToDelete);

        // when
        var teamDto = teamRepository.findTeamById(saved.getId());

        // then
        assertTrue(teamDto.isEmpty());
    }

    @Test
    @DisplayName("markTeamAsDeleted native query only affects the team with specified id")
    public void markTeamAsDeleted_SpecifiedTeamId_OnlyMarksSpecifiedTeam() {
        var saved = teamRepository.save(getTestTeamWithName("Test1"));
        teamRepository.save(getTestTeamWithName("Test2"));
        teamRepository.save(getTestTeamWithName("ASDF"));

        // when
        var countDeleted = teamRepository.markTeamAsDeleted(saved.getId());
        var team = teamRepository.findTeamById(saved.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(team.isEmpty());
    }

    @Test
    @DisplayName("markTeamAsDeleted native query only affects not deleted teams")
    public void markTeamAsDeleted_TeamAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var teamToDelete = getTestTeamWithName("Test1");
        teamToDelete.setDeleted(true);
        var saved = teamRepository.save(teamToDelete);

        // when
        Integer countDeleted = teamRepository.markTeamAsDeleted(saved.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds results which contain the specified phrase")
    public void findAllByNameContaining_MultipleTeams_OnlyFindsMatchingTeams() {
        teamRepository.save(getTestTeamWithName("Test"));
        teamRepository.save(getTestTeamWithName("Test2"));
        var saved = teamRepository.save(getTestTeamWithName("Asdf"));

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("As", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertEntityAndDtoEqual(saved, result.getContent().get(0));
    }

    @Test
    @DisplayName("findAllByNameContaining native query does not leak deleted country of a team")
    public void findAllByNameContaining_TeamWithDeletedCountry_DoesNotLeakDeletedCountry() {
        var country = new Country("Poland", "PL");
        country.setDeleted(true);
        var team = getTestTeam();
        team.setCountry(country);
        teamRepository.save(team);

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining(team.getName(), Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).getCountry());
    }

    @Test
    @DisplayName("findAllByNameContaining native query is case-insensitive")
    public void findAllByNameContaining_MultipleTeams_SearchIsCaseInsensitive() {
        teamRepository.save(getTestTeamWithName("Test"));
        teamRepository.save(getTestTeamWithName("TEST"));
        teamRepository.save(getTestTeamWithName("Asdf"));

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("Tes", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("TEST")));
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("Test")));
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds non-deleted results which contain the specified phrase")
    public void findAllByNameContaining_SomeDeletedTeams_OnlyFindsMatchingNonDeletedTeams() {
        var teamToDelete = getTestTeamWithName("Test");
        teamToDelete.setDeleted(true);
        teamRepository.save(teamToDelete);
        teamRepository.save(getTestTeamWithName("TEST"));
        teamRepository.save(getTestTeamWithName("Asdf"));

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("Test", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("TEST")));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("test", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page number information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageNumber_UsesPageableInfo() {
        var pageable = PageRequest.ofSize(6).withPage(5);

        // when
        Page<TeamDto> result = teamRepository.findAllByNameContaining("test", pageable);

        // then
        var receivedPageable = result.getPageable();
        assertEquals(6, receivedPageable.getPageSize());
        assertEquals(5, receivedPageable.getPageNumber());
    }
}
