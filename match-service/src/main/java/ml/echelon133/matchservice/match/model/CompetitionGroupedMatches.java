package ml.echelon133.matchservice.match.model;

import java.util.List;

public record CompetitionGroupedMatches(CompetitionDto competition, List<CompactMatchDto> matches) {
}
