package com.example.ATSCIRCLE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Repository.UserRepository;

@SpringBootApplication
@EnableScheduling
public class AtscircleApplication implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static void main(String[] args) {
        SpringApplication.run(AtscircleApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Admin email and password
        String adminEmail = "admin@atscircle.com";
        String adminPassword = "Admin@123"; // ✅ change this

      if (userRepository.findByEmail(adminEmail).isEmpty()) {
    Users admin = new Users();
    admin.setFirstName("System");
    admin.setLastName("SuperAdmin");
    admin.setEmail(adminEmail);
    admin.setPassword(passwordEncoder.encode(adminPassword));
    admin.setRole(Users.Role.SUPERADMIN);
    admin.setContactNumber("9999999999");
    admin.setCountryCode("+91");
    admin.setOrganizationName("ATSCircle");

    // ✅ FIX: Use enum instead of String
    admin.setOrganizationType(Users.OrganizationType.CORPORATE);

    admin.setActionStatus(Users.ActionStatus.ACTIVATED); // ✅ Direct active
    admin.setPaymentStatus(Users.PaymentStatus.FREETRIAL);

    userRepository.save(admin);

    System.out.println("✅ Default admin created: " + adminEmail + " / " + adminPassword);
} else {
    System.out.println("ℹ️ Admin already exists.");
}

    }
}
