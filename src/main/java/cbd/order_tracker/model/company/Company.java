package cbd.order_tracker.model.company;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.User;
import cbd.order_tracker.model.config.GenericConfig;
import cbd.order_tracker.model.dto.CompanyDto;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a company within the order tracking system.
 * <p>
 * A company can have its own set of configurations and is associated
 * with multiple {@link OrderRecord} entities. Each order belongs to exactly one company.
 * </p>
 *
 * <p>
 * This entity is mapped to the database using JPA annotations and is persisted
 * in the {@code company} table.
 * </p>
 */
@Entity
@Data
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** URL to the company logo */
    private String logo;

    /** Company website URL */
    private String websiteUrl;

    private String currency;

    private String vat;

    private List<String> colors;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "company_user",
            joinColumns = @JoinColumn(name = "company_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> employees = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "company_generic_config",
            joinColumns = @JoinColumn(name = "company_id"),
            inverseJoinColumns = @JoinColumn(name = "config_id")
    )
    private Set<GenericConfig> genericConfigs = new HashSet<>();

    public Company(CompanyDto companyDto) {
        this.name = companyDto.getName();
        this.logo = companyDto.getLogo();
        this.websiteUrl = companyDto.getWebsiteUrl();
        this.colors = companyDto.getColors();
        this.currency = companyDto.getCurrency();
        this.vat = companyDto.getVat();
    }
}
