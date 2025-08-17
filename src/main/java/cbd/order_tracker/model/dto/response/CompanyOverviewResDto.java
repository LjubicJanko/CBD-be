package cbd.order_tracker.model.dto.response;

import cbd.order_tracker.model.company.Company;
import lombok.Data;

@Data
public class CompanyOverviewResDto {
    private Long id;
    private String name;


    public CompanyOverviewResDto(Company company) {
        this.id = company.getId();
        this.name = company.getName();
    }
}
