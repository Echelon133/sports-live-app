package pl.echelon133.competitionservice.competition;

import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface TestUpsertGroupDto {

    static UpsertGroupDtoBuilder builder() {
        return new UpsertGroupDtoBuilder();
    }

    class UpsertGroupDtoBuilder {
        private String name = "Group " + UUID.randomUUID();
        private List<String> teams = IntStream.range(0, 20).mapToObj(i ->
                UUID.randomUUID().toString()).collect(Collectors.toList());

        private UpsertGroupDtoBuilder() {}

        public UpsertGroupDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UpsertGroupDtoBuilder teams(List<String> teams) {
            this.teams = teams;
            return this;
        }

        public UpsertCompetitionDto.UpsertGroupDto build() {
            return new UpsertCompetitionDto.UpsertGroupDto(name, teams);
        }
    }

}
