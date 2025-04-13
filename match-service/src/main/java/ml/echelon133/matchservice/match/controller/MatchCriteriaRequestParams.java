package ml.echelon133.matchservice.match.controller;

/**
 * Bundles all possible query parameters of the "GET /api/matches" endpoint to enable easier validation.
 */
public record MatchCriteriaRequestParams(String date, String utcOffset) {
}
