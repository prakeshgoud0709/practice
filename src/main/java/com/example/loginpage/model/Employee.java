package com.example.loginpage.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee")
public class Employee implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Size(min = 5, message = "Name must be at least 5 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).+$", message = "Name must include both uppercase and lowercase letters")
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is mandatory")
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{8,}$",
        message = "Password must include at least one digit, one lowercase letter, one uppercase letter, and one special character, and must be at least 8 characters long."
    )
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy; // Field to store the user who last updated the record

    @NotBlank(message = "State name is mandatory")
    private String statename;

    @ElementCollection
    @Column(name = "districtname")
    private List<String> districts;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt; // Field to store the creation timestamp

    
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;  // Field to store last update timestamp
    
    // Store history of updates (both old and new time)
    @Column(name = "updated_at")
    private String updatedAt;  // @Transient ensures it's not stored in the database

    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // Set the creation timestamp when record is created
    }

//    @PreUpdate
//    protected void onUpdate() {
//        this.updatedAt = LocalDateTime.now(); // Update the timestamp when record is updated
//    }

    public Employee(String name, String email, String password, Role role, String createdBy, String updatedBy, String updatedAt) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.updatedAt=updatedAt;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Custom logic can be applied here
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Custom logic can be applied here
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Custom logic can be applied here
    }

    @Override
    public boolean isEnabled() {
        return true; // Custom logic can be applied here
    }
}
