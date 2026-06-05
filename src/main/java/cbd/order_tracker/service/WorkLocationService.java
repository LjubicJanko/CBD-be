package cbd.order_tracker.service;

import cbd.order_tracker.model.dto.request.WorkLocationPatchRequest;
import cbd.order_tracker.model.dto.request.WorkLocationRequest;
import cbd.order_tracker.model.dto.response.WorkLocationDto;

import java.util.List;

public interface WorkLocationService {

	List<WorkLocationDto> list(Boolean activeOnly);

	WorkLocationDto create(WorkLocationRequest req);

	WorkLocationDto update(Long id, WorkLocationPatchRequest req);

	void delete(Long id);
}
