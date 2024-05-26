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

        public UpsertCompetitionDto build() {
            var dto = new UpsertCompetitionDto();
            dto.setName(name);
            dto.setSeason(season);
            dto.setLogoUrl(logoUrl);
            dto.setGroups(groups);
            dto.setLegend(legend);
            return dto;
        }
    }
}
