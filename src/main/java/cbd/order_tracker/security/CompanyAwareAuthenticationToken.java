package cbd.order_tracker.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public class CompanyAwareAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final List<Long> companyIds;
    private final boolean superAdmin;

    public CompanyAwareAuthenticationToken(
            Object principal,
            Object credentials,
            Collection<? extends GrantedAuthority> authorities,
            List<Long> companyIds,
            boolean superAdmin
    ) {
        super(principal, credentials, authorities);
        this.companyIds = companyIds;
        this.superAdmin = superAdmin;
    }

    public List<Long> getCompanyIds() {
        return companyIds;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }
}
