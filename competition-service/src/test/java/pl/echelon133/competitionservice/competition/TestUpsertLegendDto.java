package pl.echelon133.competitionservice.competition;

import pl.echelon133.competitionservice.competition.model.Legend;
import pl.echelon133.competitionservice.competition.model.UpsertCompetitionDto;

import java.util.Set;

public interface TestUpsertLegendDto {

    static UpsertLegendDtoBuilder builder() {
        return new UpsertLegendDtoBuilder();
    }

    class UpsertLegendDtoBuilder {

        private Set<Integer> positions = Set.of(1);
        private String context = "Promotion";
        private String sentiment = Legend.LegendSentiment.POSITIVE_A.toString();

        private UpsertLegendDtoBuilder() {}

        public UpsertLegendDtoBuilder positions(Set<Integer> positions) {
            this.positions = positions;
            return this;
        }

        public UpsertLegendDtoBuilder context(String context) {
            this.context = context;
            return this;
        }

        public UpsertLegendDtoBuilder sentiment(String sentiment) {
            this.sentiment = sentiment;
            return this;
        }

        public UpsertCompetitionDto.UpsertLegendDto build() {
            return new UpsertCompetitionDto.UpsertLegendDto(positions, context, sentiment);
        }
    }
}
