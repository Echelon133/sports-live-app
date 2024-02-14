package pl.echelon133.competitionservice.competition.model;

import java.util.UUID;

public class TeamDetailsDto {
    private UUID id;
    private String name;
    private String crestUrl;

    public TeamDetailsDto() {}
    public TeamDetailsDto(UUID id, String name, String crestUrl) {
        this.id = id;
        this.name = name;
        this.crestUrl = crestUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCrestUrl() {
        return crestUrl;
    }

    public void setCrestUrl(String crestUrl) {
        this.crestUrl = crestUrl;
    }
}
