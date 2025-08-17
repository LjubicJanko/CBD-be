package cbd.order_tracker.util;

import cbd.order_tracker.model.company.Company;
import cbd.order_tracker.model.dto.CompanyDto;
import cbd.order_tracker.model.dto.response.CompanyOverviewResDto;

public class CompanyMapper {
    public static CompanyDto toDto(Company company) {
        return new CompanyDto(company);
    };

    public static CompanyOverviewResDto toOverviewDto(Company company) {
        return new CompanyOverviewResDto(company);
    }
}
