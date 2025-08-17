package cbd.order_tracker.service.inter;

import cbd.order_tracker.model.dto.CompanyDto;
import cbd.order_tracker.model.dto.response.CompanyOverviewResDto;

import java.util.List;

/**
 * Service interface for managing companies in the platform.
 * <p>
 * Provides methods to retrieve company information, create new companies,
 * and update company-related data such as employee assignments.
 * </p>
 */
public interface CompanyService {

    /**
     * Retrieves an overview of all companies.
     *
     * @return a list of {@link CompanyOverviewResDto} representing all companies
     */
    List<CompanyOverviewResDto> getAll();

    /**
     * Retrieves detailed information for a specific company by ID.
     *
     * @param id the ID of the company
     * @return the {@link CompanyDto} containing detailed information
     */
    CompanyDto get(Long id);

    /**
     * Retrieves an overview of companies filtered by a list of IDs.
     *
     * @param ids the list of company IDs to retrieve
     * @return a list of {@link CompanyOverviewResDto} for the requested companies
     */
    List<CompanyOverviewResDto> getCompanies(List<Long> ids);

    /**
     * Creates a new company.
     *
     * @param companyDto the {@link CompanyDto} containing company information
     * @return the created {@link CompanyDto}
     */
    CompanyDto create(CompanyDto companyDto);

    /**
     * Updates the employees assigned to a specific company.
     *
     * @param companyId the ID of the company to update
     * @param employeeIds the list of user IDs to assign as employees
     * @return the updated {@link CompanyDto}
     */
    CompanyDto updateEmployees(Long companyId, List<Integer> employeeIds);
}
