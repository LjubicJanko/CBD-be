package cbd.order_tracker.service.inter;

import cbd.order_tracker.model.dto.request.GearReqDto;
import cbd.order_tracker.model.dto.response.GearResDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Service interface for managing Gear entities.
 * Gear represents a sports item (e.g. jersey, shorts) which belongs to a GearCategory.
 */
public interface GearService {

    /**
     * Creates a new Gear entity.
     *
     * @param gearReqDto request DTO containing name and categoryId
     * @return GearResDto containing created gear details including category info
     */
    GearResDto create(GearReqDto gearReqDto);

    /**
     * Fetches all gears.
     *
     * @return list of gears as response DTOs
     */
    List<GearResDto> getAll();

    /**
     * Fetches all gears belonging to a given category.
     *
     * @param categoryId the ID of the GearCategory (GenericConfig with type = GEAR_CATEGORY)
     * @return list of gears for the given category
     */
    List<GearResDto> getAllOfCategory(Long categoryId);

    /**
     * Updates an existing Gear entity.
     *
     * @param gearReqDto request DTO containing gear ID, updated name, and category ID
     * @return a response DTO containing the updated gear details
     * @throws EntityNotFoundException if the gear or category with the given ID does not exist
     */
    GearResDto edit(GearReqDto gearReqDto);

    /**
     * Deletes a Gear entity by its ID.
     *
     * @param id the ID of the Gear to delete
     * @throws EntityNotFoundException if the gear with the given ID does not exist
     */
    void delete(Long id);
}
