package cbd.order_tracker.service.inter;

import cbd.order_tracker.model.dto.request.PostServiceReqDto;
import java.util.List;

/**
 * Service interface for managing PostService entities.
 * <p>
 * Provides operations for creating, retrieving, updating,
 * and deleting PostService records.
 */
public interface PostServiceService {

    /**
     * Creates a new PostService entry.
     *
     * @param gearReqDto DTO containing the data for the new PostService
     * @return the created PostService DTO with any generated fields populated
     */
    PostServiceReqDto create(PostServiceReqDto gearReqDto);

    /**
     * Retrieves all PostService entries.
     *
     * @return a list of PostService DTOs
     */
    List<PostServiceReqDto> getAll();

    /**
     * Updates an existing PostService entry.
     *
     * @param gearReqDto DTO containing updated data for the PostService
     * @return the updated PostService DTO
     */
    PostServiceReqDto edit(PostServiceReqDto gearReqDto);

    /**
     * Deletes a PostService entry by its ID.
     *
     * @param id the unique identifier of the PostService to delete
     */
    void delete(Long id);
}
