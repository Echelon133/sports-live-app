package ml.echelon133.matchservice.team.model;

public class TeamFormDto {

    private Character form;
    private TeamFormDetailsDto matchDetails;

    public TeamFormDto() {}
    public TeamFormDto(Character form, TeamFormDetailsDto matchDetails) {
        this.form = form;
        this.matchDetails = matchDetails;
    }

    public Character getForm() {
        return form;
    }

    public TeamFormDetailsDto getMatchDetails() {
        return matchDetails;
    }
}
