package pl.echelon133.competitionservice.competition;

import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;

import java.util.List;

public interface TestUpsertCompetitionDto {

    static UpsertCompetitionDtoBuilder builder() {
        return new UpsertCompetitionDtoBuilder();
    }

    class UpsertCompetitionDtoBuilder {

        private String name = "Test Competition";
        private String season = "2023/24";
        private String logoUrl = "http://test.com/logo.png";
        private List<UpsertCompetitionDto.UpsertGroupDto> groups = List.of(
                TestUpsertGroupDto.builder().build(),
                TestUpsertGroupDto.builder().build()
        );
        private List<UpsertCompetitionDto.UpsertLegendDto> legend = List.of(
                TestUpsertLegendDto.builder().build()
        );
        private boolean pinned;

        private UpsertCompetitionDtoBuilder() {}

        public UpsertCompetitionDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UpsertCompetitionDtoBuilder season(String season) {
            this.season = season;
            return this;
        }

        public UpsertCompetitionDtoBuilder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public UpsertCompetitionDtoBuilder groups(List<UpsertCompetitionDto.UpsertGroupDto> groups) {
            this.groups = groups;
            return this;
        }

        public UpsertCompetitionDtoBuilder legend(List<UpsertCompetitionDto.UpsertLegendDto> legend) {
            this.legend = legend;
            return this;
        }

        public UpsertCompetitionDtoBuilder pinned(boolean pinned) {
            this.pinned = pinned;
            return this;
        }

        public UpsertCompetitionDto build() {
            return new UpsertCompetitionDto(name, season, logoUrl, groups, legend, pinned);
        }
    }
}
