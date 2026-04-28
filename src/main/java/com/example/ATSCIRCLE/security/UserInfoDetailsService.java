package com.example.ATSCIRCLE.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Models.UserManagement.Employee;
import com.example.ATSCIRCLE.Repository.EmployeeRepository;
import com.example.ATSCIRCLE.Repository.UserRepository;

@Service
public class UserInfoDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // ✅ First, check in Users table (Admin/Organization owner)
        Optional<Users> userDetail = userRepository.findByEmail(email);
        if (userDetail.isPresent()) {
            return new UserInfoDetails(userDetail.get());
        }
        
        // ✅ Then, check in Employee table
        Optional<Employee> employeeDetail = employeeRepository.findByEmail(email);
        if (employeeDetail.isPresent()) {
            return new EmployeeInfoDetails(employeeDetail.get());
        }
        
        // ✅ If not found in either table, throw exception
        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}