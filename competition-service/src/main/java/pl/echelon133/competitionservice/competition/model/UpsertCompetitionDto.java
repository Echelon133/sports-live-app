package pl.echelon133.competitionservice.competition.model;

import ml.echelon133.common.constraints.UUID;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import pl.echelon133.competitionservice.competition.model.constraints.LegendPositionsInRange;
import pl.echelon133.competitionservice.competition.model.constraints.PositionsUniqueInLegend;
import pl.echelon133.competitionservice.competition.model.constraints.SentimentValue;
import pl.echelon133.competitionservice.competition.model.constraints.TeamsUniqueInGroups;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

@LegendPositionsInRange
public class UpsertCompetitionDto {

    @NotNull
    @Length(min = 1, max = 50)
    private String name;

    @NotNull
    @Length(min = 1, max = 30)
    private String season;

    @NotNull
    @URL
    @Length(min = 15, max = 500)
    private String logoUrl;

    @Size(min = 1, max = 10)
    @TeamsUniqueInGroups
    private List<@Valid UpsertGroupDto> groups;

    @Size(max = 6)
    @PositionsUniqueInLegend
    private List<@Valid UpsertLegendDto> legend;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public List<UpsertGroupDto> getGroups() {
        return groups;
    }

    public void setGroups(List<UpsertGroupDto> groups) {
        this.groups = groups;
    }

    public List<UpsertLegendDto> getLegend() {
        return legend;
    }

    public void setLegend(List<UpsertLegendDto> legend) {
        this.legend = legend;
    }

    public static class UpsertGroupDto {

        @NotNull
        @Length(max = 50)
        private String name;

        @Size(min = 2, max = 36)
        private List<@UUID String> teams;

        public UpsertGroupDto() {}
        public UpsertGroupDto(String name, List<String> teams) {
            this.name = name;
            this.teams = teams;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getTeams() {
            return teams;
        }

        public void setTeams(List<String> teams) {
            this.teams = teams;
        }
    }

    public static class UpsertLegendDto {

        @Size(min = 1, max = 16)
        private Set<Integer> positions;

        @NotNull
        @Length(min = 1, max = 200)
        private String context;

        @NotNull
        @SentimentValue
        private String sentiment;

        public UpsertLegendDto() {}
        public UpsertLegendDto(Set<Integer> positions, String context, String sentiment) {
            this.positions = positions;
            this.context = context;
            this.sentiment = sentiment;
        }

        public Set<Integer> getPositions() {
            return positions;
        }

        public void setPositions(Set<Integer> positions) {
            this.positions = positions;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getSentiment() {
            return sentiment;
        }

        public void setSentiment(String sentiment) {
            this.sentiment = sentiment;
        }
    }
}
