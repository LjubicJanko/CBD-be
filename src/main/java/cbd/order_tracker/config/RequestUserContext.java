package cbd.order_tracker.config;

import java.util.Set;

public class RequestUserContext {
    private final Integer userId;
    private final String username;
    private final Set<Long> companyIds;
    private final Set<String> roles;

    public RequestUserContext(Integer userId, String username, Set<Long> companyIds, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.companyIds = companyIds;
        this.roles = roles;
    }

    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public Set<Long> getCompanyIds() { return companyIds; }
    public Set<String> getRoles() { return roles; }

    public boolean isSuperAdmin() {
        return roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase("super_admin"));
    }
}