package ml.echelon133.matchservice.event.model;

import ml.echelon133.common.event.dto.MatchEventDetails;
import ml.echelon133.matchservice.match.model.Match;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class MatchEvent {

    @Id
    private UUID id = UUID.randomUUID();

    @Version
    @NotNull
    private Long version;

    @CreatedDate
    private Date dateCreated;

    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Convert(converter = StringToEventDetailsConverter.class)
    @Column(length = 2000, nullable = false)
    private MatchEventDetails event;

    public MatchEvent() {}
    public MatchEvent(Match match, MatchEventDetails eventDto) {
        this.match = match;
        this.event = eventDto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public MatchEventDetails getEvent() {
        return event;
    }

    public void setEvent(MatchEventDetails event) {
        this.event = event;
    }
}
