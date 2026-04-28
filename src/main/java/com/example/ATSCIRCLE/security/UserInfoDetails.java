package com.example.ATSCIRCLE.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.ATSCIRCLE.Models.Users;

public class UserInfoDetails implements UserDetails {

    private final String username;
    private final String password;
    private final String organizationType; // ✅ custom field
    private final Collection<? extends GrantedAuthority> authorities;

    public UserInfoDetails(Users user) {
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.organizationType = user.getOrganizationType().name(); // ✅ set orgType
        this.authorities = Collections.singletonList(
                // new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                new SimpleGrantedAuthority(user.getRole().name()) 
        );
    }
    private String organizationId;
    
    // Add getter
    public String getOrganizationId() {
        return organizationId;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // 🔹 Custom getter for organizationType (NOT part of UserDetails)
    public String getOrganizationType() {
        return organizationType;
    }
}
