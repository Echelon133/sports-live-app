package ml.echelon133.common.match.dto;

public interface MatchStatusDto {
    String getStatus();

    static MatchStatusDto from(String status) {
        return () -> status;
    }
}
