package cbd.order_tracker.model.dto.request;

import cbd.order_tracker.model.config.GenericConfig;
import cbd.order_tracker.model.enumerations.ConfigType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenericConfigReqDto {
    private Long id;
    private String value;
    private ConfigType type;

    public GenericConfigReqDto(GenericConfig config) {
        this.id = config.getId();
        this.value = config.getValue();
        this.type = config.getType();
    }
}