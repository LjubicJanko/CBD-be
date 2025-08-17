package cbd.order_tracker.service;

import cbd.order_tracker.model.dto.CompanyDto;
import cbd.order_tracker.model.dto.request.UserReqDto;
import cbd.order_tracker.model.dto.response.CompanyOverviewResDto;

import java.util.List;

public interface CompanyService {

    List<CompanyOverviewResDto> getAll();

    CompanyDto get(Long id);

    List<CompanyOverviewResDto> getCompanies(List<Long> ids);

    CompanyDto create(CompanyDto companyDto);

    CompanyDto updateEmployees(Long companyId, List<Integer> employeeIds);
}
