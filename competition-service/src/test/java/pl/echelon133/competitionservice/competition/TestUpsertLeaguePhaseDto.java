package pl.echelon133.competitionservice.competition;

import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;

import java.util.List;

public interface TestUpsertLeaguePhaseDto {

    static UpsertLeaguePhaseDtoBuilder builder() {
        return new UpsertLeaguePhaseDtoBuilder();
    }

    class UpsertLeaguePhaseDtoBuilder {
        private List<UpsertCompetitionDto.UpsertGroupDto> groups =
                List.of(TestUpsertGroupDto.builder().build());
        private List<UpsertCompetitionDto.UpsertLegendDto> legend =
                List.of(TestUpsertLegendDto.builder().build());

        private UpsertLeaguePhaseDtoBuilder() {}

        public UpsertLeaguePhaseDtoBuilder groups(List<UpsertCompetitionDto.UpsertGroupDto> groups) {
            this.groups = groups;
            return this;
        }

        public UpsertLeaguePhaseDtoBuilder legend(List<UpsertCompetitionDto.UpsertLegendDto> legend) {
            this.legend = legend;
            return this;
        }

        public UpsertCompetitionDto.UpsertLeaguePhaseDto build() {
            return new UpsertCompetitionDto.UpsertLeaguePhaseDto(groups, legend);
        }
    }
}
