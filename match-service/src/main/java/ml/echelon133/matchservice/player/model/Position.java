package ml.echelon133.matchservice.player.model;

public enum Position {
    GOALKEEPER,
    DEFENDER,
    MIDFIELDER,
    FORWARD;

    public static Position valueOfIgnoreCase(String position) {
        return Position.valueOf(position.toUpperCase());
    }
}
