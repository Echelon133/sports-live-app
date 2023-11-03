package ml.echelon133.matchservice.match.controller;

/**
 * Bundles all possible query parameters of the "GET /api/matches" endpoint to enable easier validation.
 */
public class MatchCriteriaRequestParams {
    private String date;
    private String utcOffset;
    private String competitionId;
    private String type;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(String utcOffset) {
        this.utcOffset = utcOffset;
    }

    public String getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(String competitionId) {
        this.competitionId = competitionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
