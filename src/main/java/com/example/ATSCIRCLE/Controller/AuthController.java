package com.example.ATSCIRCLE.Controller;

import com.example.ATSCIRCLE.DTO.LoginDTO;
import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Service.UserService;
import com.example.ATSCIRCLE.Service.UserManagement.EmployeeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired  // ✅ FIXED: Added missing @Autowired
    private EmployeeService employeeService;

    // ✅ Register with email validation
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Users user) {
        try {
            // Input validation
            if (user == null) {
                return ResponseEntity.badRequest().body("User data cannot be null");
            }
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body("Email cannot be empty");
            }
            if (user.getOrganizationType() == null) {
                return ResponseEntity.badRequest().body("Organization type cannot be null");
            }

            // 🔍 Validate email before registering
            boolean isValid = EmailValidator.isValidEmail(user.getEmail(), user.getOrganizationType());
            if (!isValid) {
                return ResponseEntity.badRequest().body("Invalid email for organization type: " + user.getOrganizationType());
            }

            Users savedUser = userService.registerUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("Email already registered")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
            } else if (message.contains("Invalid email") || message.contains("Password must be")) {
                return ResponseEntity.badRequest().body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error during registration");
        }
    }

     // ✅ Login
     // ✅ Admin Login (Users table)
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginDTO dto) {
        try {
            // Input validation
            if (dto == null) {
                return ResponseEntity.badRequest().body("Login data cannot be null");
            }
            if (dto.getEmail() == null || dto.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body("Email cannot be empty");
            }
            if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("Password cannot be empty");
            }

            Map<String, Object> loginResponse = userService.login(dto);

            return ResponseEntity.ok(loginResponse);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("Invalid email or password")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
            } else if (message.contains("not activated") || message.contains("admin approval")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
            } else if (message.contains("cannot be empty")) {
                return ResponseEntity.badRequest().body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error during login");
        }
    }

    // ✅ Employee Login (Employee table)
    @PostMapping("/employee/login")
    public ResponseEntity<?> loginEmployee(@RequestBody LoginDTO dto) {
        try {
            // Input validation
            if (dto == null) {
                return ResponseEntity.badRequest().body("Login data cannot be null");
            }
            if (dto.getEmail() == null || dto.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body("Email cannot be empty");
            }
            if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("Password cannot be empty");
            }

            Map<String, Object> loginResponse = employeeService.loginEmployee(dto.getEmail(), dto.getPassword());

            return ResponseEntity.ok(loginResponse);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("Invalid email or password")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
            } else if (message.contains("inactive") || message.contains("contact your administrator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
            } else if (message.contains("cannot be empty")) {
                return ResponseEntity.badRequest().body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error during login");
        }
    }

     // ✅ Admin - Change User Payment Status
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/users/{id}/payment-status")
    public ResponseEntity<?> changeUserPaymentStatus(
            @PathVariable String id,
            @RequestParam(name = "paymentStatus", required = true) Users.PaymentStatus paymentStatus) {
        try {
            // Input validation
            if (id == null || id.isEmpty()) {
                return ResponseEntity.badRequest().body("User ID cannot be empty");
            }
            if (paymentStatus == null) {
                return ResponseEntity.badRequest().body("Payment status cannot be null");
            }

            Users updatedUser = userService.changeUserPaymentStatus(id, paymentStatus);
            updatedUser.setPassword(null);
            
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
            } else if (message.contains("cannot be null")) {
                return ResponseEntity.badRequest().body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update payment status: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error while updating payment status");
        }
    }
    // ✅ Admin - Get All Users
    @PreAuthorize("hasRole('SUPERADMIN')")
@GetMapping("/users")
public ResponseEntity<?> getAllAdminUsers() {
    try {
        List<Users> users = userService.getAllAdminUsers();
        return ResponseEntity.ok(users);

    } catch (RuntimeException e) {
        String message = e.getMessage();
        if (message.contains("No ADMIN users found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve users: " + message);
        }

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error while fetching users");
    }
}

     // ✅ Admin - Update Number of Users
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/users/{id}/user-count")
    public ResponseEntity<?> updateUserCount(
            @PathVariable String id,
            @RequestParam Integer noOfUsers) {
        try {
            // Input validation
            if (id == null || id.isEmpty()) {
                return ResponseEntity.badRequest().body("User ID cannot be empty");
            }
            if (noOfUsers == null || noOfUsers < 0) {
                return ResponseEntity.badRequest().body("Number of users must be a non-negative value");
            }

            Users updatedUser = userService.updateUserCount(id, noOfUsers);
            updatedUser.setPassword(null);
            
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update user count: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error while updating user count");
        }
    }
    // ✅ Admin - Change User Status
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> changeUserStatus(
            @PathVariable String id,
            @RequestParam Users.ActionStatus status) {
        try {
            // Input validation
            if (id == null || id.isEmpty()) {
                return ResponseEntity.badRequest().body("User ID cannot be empty");
            }
            if (status == null) {
                return ResponseEntity.badRequest().body("Status cannot be null");
            }

            Users updatedUser = userService.changeUserStatus(id, status);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
            } else if (message.contains("cannot be null")) {
                return ResponseEntity.badRequest().body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update user status: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error while updating user status");
        }
    }

    // ✅ Forgot Password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            // Input validation
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body("Email cannot be empty");
            }

            userService.forgotPassword(email);
            return ResponseEntity.ok("Password reset link sent to your email.");
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found in our system");
            } else if (message.contains("cannot be empty")) {
                return ResponseEntity.badRequest().body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send reset link: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error while processing forgot password request");
        }
    }

    // ✅ Reset Password
    @PostMapping("/reset-password/{id}")
    public ResponseEntity<?> resetPassword(@PathVariable String id, @RequestParam String newPassword) {
        try {
            // Input validation
            if (id == null || id.isEmpty()) {
                return ResponseEntity.badRequest().body("User ID cannot be empty");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body("New password cannot be empty");
            }
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body("Password must be at least 6 characters");
            }

            userService.updatePassword(id, newPassword);
            return ResponseEntity.ok("Password updated successfully.");
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
            } else if (message.contains("Password must be")) {
                return ResponseEntity.badRequest().body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update password: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error while resetting password");
        }
    }

    // ✅ Get My Profile
    @GetMapping("/myprofile")
    public ResponseEntity<?> getProfile(@RequestParam String email) {
        try {
            // Input validation
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body("Email cannot be empty");
            }

            Users user = userService.getProfile(email);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User profile not found");
            } else if (message.contains("cannot be empty")) {
                return ResponseEntity.badRequest().body(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve profile: " + message);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error while fetching profile");
        }
    }
    // ✅ Admin - Get Dashboard Statistics
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Map<String, Object> stats = userService.getDashboardStatistics();
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve dashboard statistics: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error while fetching dashboard statistics");
        }
    }
}