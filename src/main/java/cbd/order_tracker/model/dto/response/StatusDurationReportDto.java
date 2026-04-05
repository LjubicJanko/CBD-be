package cbd.order_tracker.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StatusDurationReportDto {
    private Long totalOrdersAnalyzed;
    private List<StatusDurationDto> statusDurations;
}
