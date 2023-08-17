package ml.echelon133.matchservice.venue.model;

import ml.echelon133.common.entity.BaseEntity;
import ml.echelon133.common.venue.dto.VenueDto;

import javax.persistence.*;
import java.util.UUID;

@NamedNativeQuery(
        name = "Venue.findVenueById",
        query = "SELECT v.id as id, v.name as name, v.capacity as capacity FROM venue v WHERE id = :id",
        resultSetMapping = "Mapping.VenueDto")
@SqlResultSetMapping(
        name = "Mapping.VenueDto",
        classes = @ConstructorResult(
                targetClass = VenueDto.class,
                columns = {
                        @ColumnResult(name = "id", type = UUID.class),
                        @ColumnResult(name = "name", type = String.class),
                        @ColumnResult(name = "capacity", type = Integer.class)
                }))
@Entity
public class Venue extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    private Integer capacity;

    public Venue() {}
    public Venue(String name, Integer capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
