package ml.echelon133.matchservice.player.repository;

import ml.echelon133.common.player.dto.PlayerDto;
import ml.echelon133.matchservice.country.model.Country;
import ml.echelon133.matchservice.player.model.Player;
import ml.echelon133.matchservice.player.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Disable kubernetes during tests
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
public class PlayerRepositoryTests {

    private final PlayerRepository playerRepository;

    @Autowired
    public PlayerRepositoryTests(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    private static Player getTestPlayer() {
        return new Player(
                "Robert Lewandowski",
                Position.FORWARD,
                LocalDate.of(1988, 8, 21),
                new Country("Poland", "PL")
        );
    }

    private static Player getTestPlayerWithName(String name) {
        return new Player(
                name,
                Position.FORWARD,
                LocalDate.of(1970, 1, 1),
                new Country("Poland", "PL")
        );
    }

    private static void assertEntityAndDtoEqual(Player playerEntity, PlayerDto playerDto) {
        // simple fields equal
        assertEquals(playerEntity.getId(), playerDto.getId());
        assertEquals(playerEntity.getName(), playerDto.getName());
        assertEquals(playerEntity.getPosition().toString(), playerDto.getPosition());
        assertEquals(playerEntity.getDateOfBirth(), playerDto.getDateOfBirth());

        // countries equal
        var playerEntityCountry = playerEntity.getCountry();
        var playerDtoCountry = playerDto.getCountry();
        assertTrue(
                playerEntityCountry.getId().equals(playerDtoCountry.getId()) &&
                playerEntityCountry.getName().equals(playerDtoCountry.getName()) &&
                playerEntityCountry.getCountryCode().equals(playerDtoCountry.getCountryCode())
        );
    }

    @Test
    @DisplayName("findPlayerById native query finds empty when the player does not exist")
    public void findPlayerById_PlayerDoesNotExist_IsEmpty() {
        var id = UUID.randomUUID();

        // when
        Optional<PlayerDto> playerDto = playerRepository.findPlayerById(id);

        // then
        assertTrue(playerDto.isEmpty());
    }

    @Test
    @DisplayName("findPlayerById native query finds player when the player exists")
    public void findPlayerById_PlayerExists_IsPresent() {
        var player = playerRepository.save(getTestPlayer());
        var saved = playerRepository.save(player);

        // when
        var playerDto = playerRepository.findPlayerById(saved.getId());

        // then
        assertTrue(playerDto.isPresent());
        assertEntityAndDtoEqual(player, playerDto.get());
    }

    @Test
    @DisplayName("findPlayerById native query finds player when the player exists and does not leak deleted country")
    public void findPlayerById_PlayerExistsAndCountryDeleted_IsPresentAndDoesNotLeakDeletedCountry() {
        var country = new Country("Poland", "PL");
        country.setDeleted(true);
        var player = getTestPlayer();
        player.setCountry(country);
        var savedPlayer = playerRepository.save(player);

        // when
        var playerDto = playerRepository.findPlayerById(savedPlayer.getId());

        // then
        assertTrue(playerDto.isPresent());
        assertNull(playerDto.get().getCountry());
    }

    @Test
    @DisplayName("findPlayerById native query does not fetch players marked as deleted")
    public void findPlayerById_PlayerMarkedAsDeleted_IsEmpty() {
        var playerToDelete = getTestPlayer();
        playerToDelete.setDeleted(true);
        var saved = playerRepository.save(playerToDelete);

        // when
        var playerDto = playerRepository.findPlayerById(saved.getId());

        // then
        assertTrue(playerDto.isEmpty());
    }

    @Test
    @DisplayName("markPlayerAsDeleted native query only affects the player with specified id")
    public void markPlayerAsDeleted_SpecifiedPlayerId_OnlyMarksSpecifiedPlayer() {
        var saved = playerRepository.save(getTestPlayerWithName("Test1"));
        playerRepository.save(getTestPlayerWithName("Test2"));
        playerRepository.save(getTestPlayerWithName("ASDF"));

        // when
        var countDeleted = playerRepository.markPlayerAsDeleted(saved.getId());
        var player = playerRepository.findPlayerById(saved.getId());

        // then
        assertEquals(1, countDeleted);
        assertTrue(player.isEmpty());
    }

    @Test
    @DisplayName("markPlayerAsDeleted native query only affects not deleted players")
    public void markPlayerAsDeleted_PlayerAlreadyMarkedAsDeleted_IsNotTouchedByQuery() {
        var playerToDelete = getTestPlayerWithName("Test1");
        playerToDelete.setDeleted(true);
        var saved = playerRepository.save(playerToDelete);

        // when
        Integer countDeleted = playerRepository.markPlayerAsDeleted(saved.getId());

        // then
        assertEquals(0, countDeleted);
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds results which contain the specified phrase")
    public void findAllByNameContaining_MultiplePlayers_OnlyFindsMatchingPlayers() {
        playerRepository.save(getTestPlayerWithName("Test"));
        playerRepository.save(getTestPlayerWithName("Test2"));
        var saved = playerRepository.save(getTestPlayerWithName("Asdf"));

        // when
        Page<PlayerDto> result = playerRepository.findAllByNameContaining("As", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertEntityAndDtoEqual(saved, result.getContent().get(0));
    }

    @Test
    @DisplayName("findAllByNameContaining native query does not leak deleted country of a player")
    public void findAllByNameContaining_PlayerWithDeletedCountry_DoesNotLeakDeletedCountry() {
        var country = new Country("Poland", "PL");
        country.setDeleted(true);
        var player = getTestPlayer();
        player.setCountry(country);
        playerRepository.save(player);

        // when
        Page<PlayerDto> result = playerRepository.findAllByNameContaining(player.getName(), Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).getCountry());
    }

    @Test
    @DisplayName("findAllByNameContaining native query is case-insensitive")
    public void findAllByNameContaining_MultiplePlayers_SearchIsCaseInsensitive() {
        playerRepository.save(getTestPlayerWithName("Test"));
        playerRepository.save(getTestPlayerWithName("TEST"));
        playerRepository.save(getTestPlayerWithName("Asdf"));

        // when
        Page<PlayerDto> result = playerRepository.findAllByNameContaining("Tes", Pageable.ofSize(10));

        // then
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("TEST")));
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("Test")));
    }

    @Test
    @DisplayName("findAllByNameContaining native query only finds non-deleted results which contain the specified phrase")
    public void findAllByNameContaining_SomeDeletedPlayers_OnlyFindsMatchingNonDeletedPlayers() {
        var playerToDelete = getTestPlayerWithName("Test");
        playerToDelete.setDeleted(true);
        playerRepository.save(playerToDelete);
        playerRepository.save(getTestPlayerWithName("TEST"));
        playerRepository.save(getTestPlayerWithName("Asdf"));

        // when
        Page<PlayerDto> result = playerRepository.findAllByNameContaining("Test", Pageable.ofSize(10));

        // then
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(p -> p.getName().equals("TEST")));
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page size information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageSize_UsesPageableInfo() {
        // when
        Page<PlayerDto> result = playerRepository.findAllByNameContaining("test", Pageable.ofSize(8));

        // then
        assertEquals(8, result.getPageable().getPageSize());
    }

    @Test
    @DisplayName("findAllByNameContaining native query takes page number information from Pageable into account")
    public void findAllByNameContaining_PageableCustomPageNumber_UsesPageableInfo() {
        var pageable = PageRequest.ofSize(6).withPage(5);

        // when
        Page<PlayerDto> result = playerRepository.findAllByNameContaining("test", pageable);

        // then
        var receivedPageable = result.getPageable();
        assertEquals(6, receivedPageable.getPageSize());
        assertEquals(5, receivedPageable.getPageNumber());
    }
}
