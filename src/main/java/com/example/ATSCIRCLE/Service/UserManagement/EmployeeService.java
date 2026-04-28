package com.example.ATSCIRCLE.Service.UserManagement;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.ATSCIRCLE.DTO.EmployeeRequestDTO;
import com.example.ATSCIRCLE.DTO.EmployeeResponseDTO;
import com.example.ATSCIRCLE.DTO.UpdatePasswordDTO;
import com.example.ATSCIRCLE.Exception.EmailAlreadyExistsException;
import com.example.ATSCIRCLE.Exception.OrganizationNotFoundException;
import com.example.ATSCIRCLE.Exception.UserLimitExceededException;
import com.example.ATSCIRCLE.Models.UserManagement.Employee;
import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Repository.EmployeeRepository;
import com.example.ATSCIRCLE.Repository.UserRepository;
import com.example.ATSCIRCLE.Service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.ATSCIRCLE.security.JwtService;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtService jwtService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int PASSWORD_LENGTH = 12;

    // ── Employee Login ────────────────────────────────────────────────────────

    public Map<String, Object> loginEmployee(String email, String password) {
        if (email == null || email.isEmpty())
            throw new RuntimeException("Email cannot be empty");
        if (password == null || password.isEmpty())
            throw new RuntimeException("Password cannot be empty");

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, employee.getPassword()))
            throw new RuntimeException("Invalid email or password");

        if (employee.getStatus() != Employee.Status.ACTIVE)
            throw new RuntimeException("Your account is inactive. Please contact your administrator.");

        String token = jwtService.generateTokenForEmployee(employee, employee.getOrganizationId());

        Map<String, Object> response = new HashMap<>();
        response.put("token",          token);
        response.put("userId",         employee.getId());
        response.put("organizationId", employee.getOrganizationId());
        response.put("email",          employee.getEmail());
        response.put("name",           employee.getName());
        response.put("role",           employee.getRole());
        response.put("phoneNumber",    employee.getPhoneNumber());
        response.put("userType",       "EMPLOYEE");
        response.put("createdBy",      employee.getCreatedBy());
        return response;
    }

    // ── Add Employee ──────────────────────────────────────────────────────────

    public EmployeeResponseDTO addUser(EmployeeRequestDTO request, String createdBy) {

        if (employeeRepository.existsByEmail(request.getEmail()))
            throw new EmailAlreadyExistsException("Email already exists");

        Users orgUsers = usersRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

        long activeEmployeeCount = employeeRepository
                .countByOrganizationIdAndStatus(request.getOrganizationId(), Employee.Status.ACTIVE);

        if (activeEmployeeCount >= orgUsers.getNoOfUsers()) {
            throw new UserLimitExceededException(
                    "User limit exceeded. Maximum allowed users: " + orgUsers.getNoOfUsers());
        }

        String randomPassword = generateRandomPassword();

        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setPassword(passwordEncoder.encode(randomPassword));
        employee.setRole(request.getRole());
        employee.setOrganizationId(request.getOrganizationId());
        employee.setCreatedBy(createdBy);
        employee.setStatus(Employee.Status.ACTIVE);

        Employee savedEmployee = employeeRepository.save(employee);

        // ✅ Pass organizationId so the welcome email is sent FROM the org's
        //    connected Gmail / Outlook account (not the system SMTP).
        //    Falls back to SMTP automatically if no provider is configured.
        emailService.sendCredentialsEmailForOrg(
                savedEmployee.getOrganizationId(),   // ← routes via org's connected account
                savedEmployee.getEmail(),
                savedEmployee.getName(),
                savedEmployee.getEmail(),
                randomPassword
        );

        System.out.println("📧 Credentials email dispatched for employee: "
                + savedEmployee.getEmail()
                + " via org: " + savedEmployee.getOrganizationId());

        return mapToResponseDTO(savedEmployee);
    }

    // ── Update Password ───────────────────────────────────────────────────────

    public EmployeeResponseDTO updatePassword(String employeeId, UpdatePasswordDTO updatePasswordDTO) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + employeeId));

        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), employee.getPassword()))
            throw new IllegalArgumentException("Old password is incorrect");

        employee.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        return mapToResponseDTO(employeeRepository.save(employee));
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<EmployeeResponseDTO> getAllUsers(String organizationId) {
        return employeeRepository.findByOrganizationId(organizationId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public EmployeeResponseDTO getUserById(String id) {
        Optional<Employee> employeeOpt = employeeRepository.findById(id);
        if (employeeOpt.isPresent()) {
            return mapToResponseDTO(employeeOpt.get());
        } else {
            System.out.println("❌ Employee NOT FOUND with id: " + id);
            return null;
        }
    }

    public EmployeeResponseDTO updateUser(String id, EmployeeRequestDTO request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + id));

        if (request.getName() != null && !request.getName().isEmpty())
            employee.setName(request.getName());

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!employee.getEmail().equals(request.getEmail())
                    && employeeRepository.existsByEmail(request.getEmail()))
                throw new IllegalArgumentException("Email already exists");
            employee.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty())
            employee.setPhoneNumber(request.getPhoneNumber());

        if (request.getRole() != null)
            employee.setRole(request.getRole());

        return mapToResponseDTO(employeeRepository.save(employee));
    }

    public void deleteUser(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }

    public EmployeeResponseDTO inactivateUser(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + id));
        employee.setStatus(Employee.Status.INACTIVE);
        return mapToResponseDTO(employeeRepository.save(employee));
    }

    public EmployeeResponseDTO activateUser(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + id));
        employee.setStatus(Employee.Status.ACTIVE);
        return mapToResponseDTO(employeeRepository.save(employee));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    private EmployeeResponseDTO mapToResponseDTO(Employee employee) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setPhoneNumber(employee.getPhoneNumber());
        dto.setRole(employee.getRole());
        dto.setOrganizationId(employee.getOrganizationId());
        dto.setCreatedBy(employee.getCreatedBy());
        dto.setStatus(employee.getStatus());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setUpdatedAt(employee.getUpdatedAt());
        return dto;
    }
}