package cbd.order_tracker.service.inter;

import cbd.order_tracker.model.config.GenericConfig;
import cbd.order_tracker.model.dto.request.GenericConfigReqDto;
import cbd.order_tracker.model.dto.response.GenericConfigResDto;
import cbd.order_tracker.model.enumerations.ConfigType;

import java.util.List;

/**
 * Service interface for managing generic platform configurations.
 * <p>
 * A generic configuration represents a configurable item that can be assigned
 * to companies. This service allows CRUD operations as well as bulk updates
 * of configurations by type.
 * </p>
 */
public interface GenericConfigService {

    /**
     * Retrieves all generic configurations from the platform.
     *
     * @return a list of {@link GenericConfigResDto} representing all configurations
     */
    List<GenericConfigResDto> getAll();

    /**
     * Retrieves all generic configurations filtered by type.
     *
     * @param type the type of configurations to retrieve
     * @return a list of {@link GenericConfigResDto} for the given type
     */
    List<GenericConfigResDto> getByType(ConfigType type);

    /**
     * Creates a new generic configuration.
     * <p>
     * Typically used by a super admin to add new options that can later
     * be assigned to companies.
     * </p>
     *
     * @param config the configuration request DTO containing type and value
     * @return the created {@link GenericConfigResDto}
     */
    GenericConfigResDto create(GenericConfigReqDto config);

    /**
     * Updates an existing generic configuration by its ID.
     *
     * @param id the ID of the configuration to update
     * @param updatedConfig the request DTO containing the new values
     * @return the updated {@link GenericConfigResDto}
     */
    GenericConfigResDto update(Long id, GenericConfigReqDto updatedConfig);

    /**
     * Bulk updates all configurations of a specific type.
     * <p>
     * Existing configurations of this type will be overridden with the provided list.
     * This is useful for quickly replacing all options of a given type.
     * </p>
     *
     * @param type the type of configurations to override
     * @param configs the list of new configuration request DTOs
     * @return the list of updated {@link GenericConfigResDto}
     */
    List<GenericConfigResDto> bulkUpdate(String type, List<GenericConfigReqDto> configs);

    /**
     * Deletes a generic configuration by its ID.
     *
     * @param id the ID of the configuration to delete
     */
    void delete(Long id);
}
