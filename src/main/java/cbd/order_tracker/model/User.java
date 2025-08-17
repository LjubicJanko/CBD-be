package cbd.order_tracker.model;

import cbd.order_tracker.model.company.Company;
import cbd.order_tracker.model.dto.RegisterUserDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Table(name = "users")
@Entity
@Data
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, length = 100, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(mappedBy = "employees", fetch = FetchType.LAZY)
    private Set<Company> companies = new HashSet<>();

    public User() {}

    public User(String fullName, String username, String password) {
        this.fullName = fullName;
        this.username = username;
        this.password = password;
    }

    public User(RegisterUserDto registerUserDto, Role role, String encodedPassword) {
        this.fullName = registerUserDto.getFullName();
        this.username = registerUserDto.getUsername();
        this.password = encodedPassword;
        this.roles = new HashSet<>(Collections.singleton(role));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            roles.forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                if (role.getPrivileges() != null) {
                    role.getPrivileges().forEach(privilege ->
                            authorities.add(new SimpleGrantedAuthority(privilege.getName()))
                    );
                }
            });
        }
        return authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    public void setRoles(Set<Role> roles) { this.roles = roles != null ? roles : new HashSet<>(); }

    @Override
    public String toString() {
        return "User{id=" + id + ", fullName='" + fullName + "', username='" + username + "', createdAt=" + createdAt + ", updatedAt=" + updatedAt + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}