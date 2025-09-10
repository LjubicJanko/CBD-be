package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.config.Gear;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GearResDto {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Long typeId;
    private String typeName;

    public GearResDto(Gear gear) {
        this.id = gear.getId();
        this.name = gear.getName();
        if (gear.getCategory() != null) {
            this.categoryId = gear.getCategory().getId();
            this.categoryName = gear.getCategory().getValue();
        }
        if(gear.getType() != null) {
            this.typeId = gear.getType().getId();
            this.typeName = gear.getType().getValue();
        }
    }
}
