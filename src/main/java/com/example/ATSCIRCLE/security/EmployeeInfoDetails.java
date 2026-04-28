package com.example.ATSCIRCLE.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.ATSCIRCLE.Models.UserManagement.Employee;

public class EmployeeInfoDetails implements UserDetails {

    private final String username;
    private final String password;
    private final String organizationId;
    private final Collection<? extends GrantedAuthority> authorities;

    public EmployeeInfoDetails(Employee employee) {
        this.username = employee.getEmail();
        this.password = employee.getPassword();
        this.organizationId = employee.getOrganizationId();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())
        );
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

    public String getOrganizationId() {
        return organizationId;
    }
}