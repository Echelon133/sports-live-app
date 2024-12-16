package ml.echelon133.matchservice.match;

import ml.echelon133.matchservice.match.model.LineupDto;
import ml.echelon133.matchservice.team.model.TeamPlayerDto;
import ml.echelon133.matchservice.team.TestTeamPlayerDto;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public interface TestLineupDto {

    static LineupDtoBuilder builder() {
        return new LineupDtoBuilder();
    }

    class LineupDtoBuilder {
        private List<TeamPlayerDto> homeStarting = List.of();
        private List<TeamPlayerDto> homeSubstitutes = List.of();
        private List<TeamPlayerDto> awayStarting = List.of();
        private List<TeamPlayerDto> awaySubstitutes = List.of();
        private String homeFormation = "";
        private String awayFormation = "";

        private LineupDtoBuilder() {}

        private List<TeamPlayerDto> idsToTestTeamPlayerDto(UUID... testPlayerIds) {
            return Arrays.stream(testPlayerIds)
                    .map(id -> TestTeamPlayerDto.builder().id(id).build()).collect(Collectors.toList());
        }

        public LineupDtoBuilder homeStarting(UUID... testPlayerIds) {
            this.homeStarting = idsToTestTeamPlayerDto(testPlayerIds);
            return this;
        }

        public LineupDtoBuilder homeSubstitutes(UUID... testPlayerIds) {
            this.homeSubstitutes = idsToTestTeamPlayerDto(testPlayerIds);
            return this;
        }

        public LineupDtoBuilder awayStarting(UUID... testPlayerIds) {
            this.awayStarting = idsToTestTeamPlayerDto(testPlayerIds);
            return this;
        }

        public LineupDtoBuilder awaySubstitutes(UUID... testPlayerIds) {
            this.awaySubstitutes = idsToTestTeamPlayerDto(testPlayerIds);
            return this;
        }

        public LineupDtoBuilder homeFormation(String homeFormation) {
            this.homeFormation = homeFormation;
            return this;
        }

        public LineupDtoBuilder awayFormation(String awayFormation) {
            this.awayFormation = awayFormation;
            return this;
        }

        public LineupDto build() {
            return new LineupDto(
                new LineupDto.TeamLineup(homeStarting, homeSubstitutes, homeFormation),
                new LineupDto.TeamLineup(awayStarting, awaySubstitutes, awayFormation)
            );
        }
    }
}
