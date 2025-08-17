package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.config.GenericConfig;
import cbd.order_tracker.model.enumerations.ConfigType;
import lombok.Data;

@Data
public class GenericConfigResDto {
    private Long id;
    private String value;
    private ConfigType type;

    public GenericConfigResDto(GenericConfig config) {
        this.id = config.getId();
        this.value = config.getValue();
        this.type = config.getType();
    }
}
