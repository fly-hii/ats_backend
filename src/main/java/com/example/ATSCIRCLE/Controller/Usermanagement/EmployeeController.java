package com.example.ATSCIRCLE.Controller.Usermanagement;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.ATSCIRCLE.DTO.UpdatePasswordDTO;
import com.example.ATSCIRCLE.DTO.EmployeeRequestDTO;
import com.example.ATSCIRCLE.DTO.EmployeeResponseDTO;
import com.example.ATSCIRCLE.Service.UserManagement.EmployeeService;
import com.example.ATSCIRCLE.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private JwtService jwtService;

    @PostMapping("/addEmployee")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGEMENT')")
    public ResponseEntity<EmployeeResponseDTO> addUser(
            @RequestBody EmployeeRequestDTO request,
            HttpServletRequest httpRequest) {
        
        // ✅ Extract userId from JWT token
        String token = extractToken(httpRequest);
        String createdBy = jwtService.extractOrganizationId(token);
        System.out.println(token);
        System.out.println(createdBy);
        
        EmployeeResponseDTO response = employeeService.addUser(request, createdBy);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponseDTO> updatePassword(
            @PathVariable String id,
            @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        EmployeeResponseDTO response = employeeService.updatePassword(id, updatePasswordDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGEMENT')")
    public ResponseEntity<List<EmployeeResponseDTO>> getAllUsers(HttpServletRequest httpRequest) {
        // ✅ Extract organizationId from JWT token
        String token = extractToken(httpRequest);
        String organizationId = jwtService.extractOrganizationId(token);
        
        List<EmployeeResponseDTO> employees = employeeService.getAllUsers(organizationId);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> getUserById(@PathVariable String id) {

    EmployeeResponseDTO employee = employeeService.getUserById(id);

    if (employee == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("message",
                        "Employee not found with id: " + id));
    }

    return ResponseEntity.ok(employee);
}


    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponseDTO> updateUser(
            @PathVariable String id,
            @RequestBody EmployeeRequestDTO request) {
        EmployeeResponseDTO response = employeeService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGEMENT')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        employeeService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGEMENT')")
    public ResponseEntity<EmployeeResponseDTO> inactivateUser(@PathVariable String id) {
        EmployeeResponseDTO response = employeeService.inactivateUser(id);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGEMENT')")
    public ResponseEntity<EmployeeResponseDTO> activateUser(@PathVariable String id) {
        EmployeeResponseDTO response = employeeService.activateUser(id);
        return ResponseEntity.ok(response);
    }

    // ✅ Helper method to extract token from Authorization header
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("No token found in request");
    }
}