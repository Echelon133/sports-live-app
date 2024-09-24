package ml.echelon133.matchservice.match.model;

import java.io.Serializable;

public interface RedCardInfoDto extends Serializable {
    Integer getHomeRedCards();
    Integer getAwayRedCards();

    static RedCardInfoDto from(Integer homeRedCards, Integer awayRedCards) {
        return new RedCardInfoDto() {
            @Override
            public Integer getHomeRedCards() {
                return homeRedCards;
            }

            @Override
            public Integer getAwayRedCards() {
                return awayRedCards;
            }
        };
    }
}
