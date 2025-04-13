package pl.echelon133.competitionservice.competition.model;

import java.util.UUID;

public interface LabeledMatch {
    String getLabel();
    UUID getMatchId();

    static LabeledMatch from(String label, UUID matchId) {
        return new LabeledMatch() {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public UUID getMatchId() {
                return matchId;
            }
        };
    }
}
