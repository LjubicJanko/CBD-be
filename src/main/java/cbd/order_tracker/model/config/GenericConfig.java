package cbd.order_tracker.model.config;

import cbd.order_tracker.model.dto.request.GenericConfigReqDto;
import cbd.order_tracker.model.enumerations.ConfigType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic company configuration entity that is distinguished by type, and defined by simple string value.
 */
@Entity
@Table(name = "generic_config")
@Data
@NoArgsConstructor
public class GenericConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConfigType type;


    @Column(nullable = false)
    private String value;

    public GenericConfig(GenericConfigReqDto reqConfig) {
        this.type = reqConfig.getType();
        this.value = reqConfig.getValue();
    }

}
