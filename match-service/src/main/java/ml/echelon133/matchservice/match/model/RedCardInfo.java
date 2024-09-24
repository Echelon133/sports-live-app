package ml.echelon133.matchservice.match.model;

import javax.persistence.Embeddable;

@Embeddable
public class RedCardInfo {
    private Integer homeRedCards;
    private Integer awayRedCards;

    public RedCardInfo() {
        this.homeRedCards = 0;
        this.awayRedCards = 0;
    }

    public RedCardInfo(Integer homeRedCards, Integer awayRedCards) {
        this.homeRedCards = homeRedCards;
        this.awayRedCards = awayRedCards;
    }

    public Integer getHomeRedCards() {
        return homeRedCards;
    }

    public void setHomeRedCards(Integer homeRedCards) {
        this.homeRedCards = homeRedCards;
    }

    public Integer getAwayRedCards() {
        return awayRedCards;
    }

    public void setAwayRedCards(Integer awayRedCards) {
        this.awayRedCards = awayRedCards;
    }

    public void incrementHomeCards() {
        ++this.homeRedCards;
    }

    public void incrementAwayCards() {
        ++this.awayRedCards;
    }

    public static RedCardInfo of(Integer homeRedCards, Integer awayRedCards) {
        return new RedCardInfo(homeRedCards, awayRedCards);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RedCardInfo redCardInfo = (RedCardInfo) o;

        if (!homeRedCards.equals(redCardInfo.homeRedCards)) return false;
        return awayRedCards.equals(redCardInfo.awayRedCards);
    }

    @Override
    public int hashCode() {
        int result = homeRedCards.hashCode();
        result = 31 * result + awayRedCards.hashCode();
        return result;
    }
}
