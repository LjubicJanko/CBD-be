package cbd.order_tracker.model.config;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="gear")
@Data
@NoArgsConstructor
public class Gear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
//    todo add type

    // Link to GenericConfig where type = GEAR_CATEGORY
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private GenericConfig category;

    // Link to GenericConfig where type = GEAR_TYPE
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private GenericConfig type;
}
