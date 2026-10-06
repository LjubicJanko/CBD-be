package cbd.order_tracker.model.dto.request;

import lombok.Data;

@Data
public class EditPrintFilesUrlDto {
    // null or blank clears the link
    private String printFilesUrl;
}
