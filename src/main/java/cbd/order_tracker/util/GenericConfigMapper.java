package cbd.order_tracker.util;

import cbd.order_tracker.model.config.GenericConfig;
import cbd.order_tracker.model.dto.response.GenericConfigResDto;

public class GenericConfigMapper {
    public static GenericConfigResDto toResDto(GenericConfig config) {
        return new GenericConfigResDto(config);
    }

}
