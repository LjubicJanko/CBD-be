package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.company.Company;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CompanyDto {
    private Long id;
    private String name;
    private String logo;
    private String currency;
    private String vat;
    private List<String> colors;
    private String websiteUrl;

    public CompanyDto(Company company) {
        this.id = company.getId();
        this.name = company.getName();
        this.logo = company.getLogo();
        this.websiteUrl = company.getWebsiteUrl();
        this.vat = company.getVat();
        this.currency = company.getCurrency();
        this.colors = company.getColors();
    }
}
