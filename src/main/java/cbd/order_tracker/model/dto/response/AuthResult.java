package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.Role;
import cbd.order_tracker.model.User;
import java.util.List;

public record AuthResult(
        User user,
        List<Long> companyIds
) {}