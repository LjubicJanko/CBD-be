package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.company.Company;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CompanyDto {
    private Long id;
    private String name;

    public CompanyDto(Company company) {
        this.id = company.getId();
        this.name = company.getName();
    }
}
