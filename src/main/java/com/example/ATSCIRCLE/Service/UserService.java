package com.example.ATSCIRCLE.Service;

import com.example.ATSCIRCLE.Controller.EmailValidator;
import com.example.ATSCIRCLE.DTO.LoginDTO;
import com.example.ATSCIRCLE.Models.Users;
import com.example.ATSCIRCLE.Repository.UserRepository;
import com.example.ATSCIRCLE.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailService emailService; // ✅ Mail service (create separately)

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ✅ Registration
public Users registerUser(Users user) {
    // Email validation
    if (!EmailValidator.isValidEmail(user.getEmail(), user.getOrganizationType())) {
        throw new RuntimeException("Invalid email for organization type: " + user.getOrganizationType());
    }

    // Duplicate email check
    if (userRepository.findByEmail(user.getEmail()).isPresent()) {
        throw new RuntimeException("Email already registered");
    }

    // Password validation
    if (user.getPassword() == null || user.getPassword().length() < 6) {
        throw new RuntimeException("Password must be at least 6 characters");
    }

    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setRole(Users.Role.ADMIN);
    user.setActionStatus(Users.ActionStatus.PENDING);

    Users saved = userRepository.save(user);

    // ✅ Send HTML mail on registration
String htmlTemplate = """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width">
  <title>Registration Confirmation - ATS Circle</title>
</head>
<body style="margin:0;padding:0;background:#f4f6f8;font-family:'Helvetica Neue',Arial,sans-serif;">
  <!-- Outer wrapper -->
  <table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="background:#f4f6f8;padding:40px 0;">
    <tr>
      <td align="center">
        <!-- Card -->
        <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="max-width:600px;width:100%;background:#ffffff;border-radius:12px;box-shadow:0 6px 18px rgba(15,23,42,0.08);overflow:hidden;">
          
          <!-- Top colored strip -->
          <tr>
            <td style="background:linear-gradient(90deg,#0ea5a0,#2563eb);height:8px;padding:0;"></td>
          </tr>
 
          <!-- Header / Logo -->
          <tr>
            <td style="padding:28px 36px 8px 36px;text-align:left;">
              <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                <tr>
                  <td style="vertical-align:middle;">
                    <table role="presentation" cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="width:48px;height:48px;border-radius:50%;background:#2563eb;text-align:center;vertical-align:middle;">
                          <span style="display:inline-block;font-size:16px;font-weight:700;color:#ffffff;line-height:48px;">ATS</span>
                        </td>
                        <td style="padding-left:10px;vertical-align:middle;">
                          <span style="font-size:20px;font-weight:700;color:#0f172a;">CIRCLE</span>
                        </td>
                      </tr>
                    </table>
                  </td>
                  <td style="text-align:right;vertical-align:middle;font-size:13px;color:#94a3b8;">
                    <span style="display:inline-block;padding:6px 10px;border-radius:999px;background:#eef2ff;color:#3730a3;font-weight:600;">Welcome!</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
 
          <!-- Hero / Message -->
          <tr>
            <td style="padding:18px 36px 28px 36px;">
              <h1 style="margin:0 0 12px 0;font-size:22px;line-height:1.2;color:#0f172a;font-weight:700;">
                Thank you for joining ATS Circle
              </h1>
              <p style="margin:0 0 20px 0;color:#475569;font-size:15px;line-height:1.6;">
                Hi <strong>{{name}}</strong>,<br>
                We’re excited to have you on board! Your registration has been received successfully. 
                You’ll get an activation email shortly.
              </p>

              <table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="border:1px solid #eef2ff;border-radius:8px;padding:12px;background:#fbfdff;">
                <tr>
                  <td style="vertical-align:middle;padding-right:12px;width:52px;">
                    <div style="width:44px;height:44px;border-radius:10px;background:linear-gradient(180deg,#f8fafc,#eef2ff);display:flex;align-items:center;justify-content:center;font-weight:700;color:#0b74ff;text-align:center;line-height:44px;">
                      ✔
                    </div>
                  </td>
                  <td style="vertical-align:middle;color:#334155;font-size:14px;">
                    Account status: <strong style="color:#0f172a;">Pending activation</strong><br>
                    Expected next step: You will receive an activation email from us.
                  </td>
                </tr>
              </table>

              <div style="margin-top:24px;">
                <h3 style="margin:0 0 12px 0;font-size:16px;color:#0f172a;">What’s Next?</h3>
                <ol style="margin:0;padding-left:20px;color:#475569;font-size:14px;line-height:1.6;">
                  <li>Check your inbox for an activation email.</li>
                  <li>Click the activation link provided.</li>
                  <li>Login and start exploring ATS Circle.</li>
                </ol>
              </div>
            </td>
          </tr>
 
          <!-- Footer -->
          <tr>
            <td style="padding:20px 24px;font-size:12px;color:#94a3b8;text-align:center;background:#f9fafb;">
              <p style="margin:0;">Need help? Contact us at 
                <a href="mailto:support@atscircle.com" style="color:#2563eb;text-decoration:none;">support@atscircle.com</a>
              </p>
              <p style="margin:6px 0 0 0;">&copy; 2025 ATS Circle. All rights reserved.</p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""";

String htmlContent =  htmlTemplate.replace("{{name}}", user.getFirstName());

// ✅ send HTML mail
// emailService.sendHtmlEmail(user.getEmail(), "Welcome to ATSCIRCLE 🎉", htmlContent);
    try {
    emailService.sendHtmlEmail(user.getEmail(), "Welcome to ATSCIRCLE 🎉", htmlContent);
    System.out.println("✅ Registration email sent to: " + user.getEmail());
} catch (Exception e) {
    System.err.println("⚠️ Failed to send registration email: " + e.getMessage());
    // Don't rethrow — registration already succeeded
}

    return saved;
}


    // ✅ Login
    public Map<String, Object> login(LoginDTO dto) {
        // Email validation
        if (dto.getEmail() == null || dto.getEmail().isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }

        // Password validation
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }

        Users user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (user.getActionStatus() != Users.ActionStatus.ACTIVATED) {
            throw new RuntimeException("Your account is not activated. Please wait for admin approval.");
        }

        String token = jwtService.generateToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("OrganizationId", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("role", user.getRole());
        response.put("organizationType", user.getOrganizationType());
        response.put("organizationName", user.getOrganizationName());
        response.put("paymentStatus", user.getPaymentStatus());
        response.put("noOfUsers", user.getNoOfUsers());

        return response;
    }

    // ✅ Get All Users (Admin only)
   public List<Users> getAllAdminUsers() {
    List<Users> users = userRepository.findByRole("ADMIN");

    if (users.isEmpty()) {
        throw new RuntimeException("No ADMIN users found");
    }

    return users;
}

   // ✅ Change Action Status (Admin)
public Users changeUserStatus(String userId, Users.ActionStatus status) {
    if (status == null) {
        throw new RuntimeException("Status cannot be null");
    }

    Users user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    user.setActionStatus(status);
    userRepository.save(user);

    // ✅ Mail on activation with HTML template
    if (status == Users.ActionStatus.ACTIVATED) {
        String activationTemplate = """
                <!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width">
  <title>ATS Circle - Activation</title>
</head>
<body style="margin:0;padding:0;background:#f4f6f8;font-family:'Helvetica Neue', Arial, sans-serif;">
  <!-- Outer wrapper -->
  <table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="background:#f4f6f8;padding:40px 0;">
    <tr>
      <td align="center">
        <!-- Card -->
        <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="max-width:600px;width:100%;background:#ffffff;border-radius:12px;box-shadow:0 6px 18px rgba(15,23,42,0.08);overflow:hidden;">
          <!-- Top colored strip -->
          <tr>
            <td style="background:linear-gradient(90deg,#0ea5a0,#2563eb);height:8px;padding:0;"></td>
          </tr>
 
          <!-- Header / Logo -->
          <tr>
            <td style="padding:28px 36px 8px 36px;text-align:left;">
              <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                <tr>
                  <td style="vertical-align:middle;">
                    <table role="presentation" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
                      <tr>
                        <td style="width:48px;height:48px;border-radius:50%;background:#2563eb;text-align:center;vertical-align:middle;">
                          <span style="display:inline-block;font-size:16px;font-weight:700;color:#ffffff;line-height:48px;">ATS</span>
                        </td>
                        <td style="padding-left:10px;vertical-align:middle;">
                          <span style="font-size:20px;font-weight:700;color:#0f172a;">CIRCLE</span>
                        </td>
                      </tr>
                    </table>
                  </td>
                  <td style="text-align:right;vertical-align:middle;font-size:13px;color:#94a3b8;">
                    <span style="display:inline-block;padding:6px 10px;border-radius:999px;background:#eef2ff;color:#3730a3;font-weight:600;">Welcome!</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
 
          <!-- Hero / Message -->
          <tr>
            <td style="padding:18px 36px 28px 36px;">
              <h1 style="margin:0 0 12px 0;font-size:22px;line-height:1.3;color:#0f172a;font-weight:700;">
                A Heartfelt Welcome to ATS Circle! - Our Journey Starts Now!
              </h1>
 
              <p style="margin:0 0 20px 0;font-size:15px;line-height:1.6;color:#475569;">
                Dear <strong>{{FirstName}}</strong>,
              </p>
 
              <p style="margin:0 0 20px 0;font-size:15px;line-height:1.6;color:#475569;">
                Welcome to the ATS Circle family! We're absolutely thrilled to have you on board. Your decision to join us marks the start of an exciting journey together, focused on achieving your goals and making your experience truly exceptional.
              </p>
 
              <p style="margin:0 0 20px 0;font-size:15px;line-height:1.6;color:#475569;">
                As you step into our world of integrated solutions, where the cutting-edge Applicant Tracking System meets dynamic Sales CRM, remember that you're not just a user, but a valued member of our community.
              </p>
 
              <p style="margin:0 0 20px 0;font-size:15px;line-height:1.6;color:#475569;">
                Every success you achieve using our platform resonates deeply with us. Your wins are our wins, and we're here to celebrate each milestone, big or small.
              </p>
 
              <!-- Call-to-action button -->
              <div style="margin:24px 0;">
                <a href="https://at-sfrontend1.vercel.app/" style="display:inline-block;padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:600;border:1px solid #2563eb;background:#2563eb;color:#ffffff;">
                  Login to Your Account
                </a>
              </div>
 
              <p style="margin:0 0 20px 0;font-size:15px;line-height:1.6;color:#475569;">
                Feel free to explore, experiment, and engage with our tools to unlock your full potential. And if at any point you need guidance or support, please don't hesitate to reply to this email or contact our support team at 
                <a href="mailto:support@atscircle.com" style="color:#2563eb;text-decoration:none;">support@atscircle.com</a>. Our dedicated team is here to assist you.
              </p>
 
              <p style="margin:0;font-size:15px;line-height:1.6;color:#475569;">
                Warmest wishes,<br>
                <strong>ATS Circle Team</strong>
              </p>
            </td>
          </tr>
 
          <!-- Footer -->
         <tr>
              <td style="padding:20px 24px;font-size:12px;color:#94a3b8;text-align:center;background:#f9fafb;">
                <p style="margin:0;">Need help? Contact us at 
                  <a href="mailto:support@atscircle.com" style="color:#2563eb;text-decoration:none;">support@example.com</a>
                </p>
                <p style="margin:6px 0 0 0;">&copy; 2025 ATS Circle. All rights reserved.</p>
              </td>
            </tr>
 
          </table>
        </td>
      </tr>

 
        </table>
        <!-- End card -->
      </td>
    </tr>
  </table>
</body>
</html>
                """;

      String loginUrl = "https://at-sfrontend1.vercel.app/";

    // Replace placeholders safely
    String htmlContent = activationTemplate
        .replace("{{FirstName}}", user.getFirstName())
        .replace("{{LoginUrl}}", loginUrl); // if you add {{LoginUrl}} in template

    emailService.sendHtmlEmail(
        user.getEmail(),
        "Account Activated - Welcome to ATS Circle!",
        htmlContent
    );
    }

    return user;
}
   // ✅ Update User Count (Admin)
    public Users updateUserCount(String userId, Integer noOfUsers) {
        if (noOfUsers == null || noOfUsers < 0) {
            throw new RuntimeException("Number of users must be a non-negative value");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setNoOfUsers(noOfUsers);
        return userRepository.save(user);
    }

 // ✅ Get Dashboard Statistics (Admin only)
    public Map<String, Object> getDashboardStatistics() {
        List<Users> allUsers = userRepository.findAll();
        
        // Filter out SUPERADMIN users from counts
        List<Users> regularUsers = allUsers.stream()
                .filter(user -> user.getRole() != Users.Role.SUPERADMIN)
                .toList();
        
        long totalUsers = regularUsers.size();
        
        long activeUsers = regularUsers.stream()
                .filter(user -> user.getActionStatus() == Users.ActionStatus.ACTIVATED)
                .count();
        
        long pendingUsers = regularUsers.stream()
                .filter(user -> user.getActionStatus() == Users.ActionStatus.PENDING)
                .count();
        
        long paidUsers = regularUsers.stream()
                .filter(user -> user.getPaymentStatus() == Users.PaymentStatus.PAID)
                .count();
        
        long unpaidUsers = regularUsers.stream()
                .filter(user -> user.getPaymentStatus() == Users.PaymentStatus.UNPAID)
                .count();
        
        long freeTrialUsers = regularUsers.stream()
                .filter(user -> user.getPaymentStatus() == Users.PaymentStatus.FREETRIAL)
                .count();
        
        long deactivatedUsers = regularUsers.stream()
                .filter(user -> user.getActionStatus() == Users.ActionStatus.DEACTIVATED)
                .count();
        
        long onHoldUsers = regularUsers.stream()
                .filter(user -> user.getActionStatus() == Users.ActionStatus.ON_HOLD)
                .count();
        
        long suspendedUsers = regularUsers.stream()
                .filter(user -> user.getActionStatus() == Users.ActionStatus.SUSPENDED)
                .count();
        
        long bannedUsers = regularUsers.stream()
                .filter(user -> user.getActionStatus() == Users.ActionStatus.BANNED)
                .count();
        
        // Count by organization type
        long freelancers = regularUsers.stream()
                .filter(user -> user.getOrganizationType() == Users.OrganizationType.FREELANCER)
                .count();
        
        long staffingAgencies = regularUsers.stream()
                .filter(user -> user.getOrganizationType() == Users.OrganizationType.STAFFING_AGENCY)
                .count();
        
        long corporates = regularUsers.stream()
                .filter(user -> user.getOrganizationType() == Users.OrganizationType.CORPORATE)
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("pendingUsers", pendingUsers);
        stats.put("paidUsers", paidUsers);
        stats.put("unpaidUsers", unpaidUsers);
        stats.put("freeTrialUsers", freeTrialUsers);
        stats.put("deactivatedUsers", deactivatedUsers);
        stats.put("onHoldUsers", onHoldUsers);
        stats.put("suspendedUsers", suspendedUsers);
        stats.put("bannedUsers", bannedUsers);
        stats.put("freelancers", freelancers);
        stats.put("staffingAgencies", staffingAgencies);
        stats.put("corporates", corporates);
        
        return stats;
    }


       // ✅ Change Payment Status (Admin)
    public Users changeUserPaymentStatus(String userId, Users.PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            throw new RuntimeException("Payment status cannot be null");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPaymentStatus(paymentStatus);
        return userRepository.save(user);
    }
   // ✅ Forgot Password
public void forgotPassword(String email) {
    if (email == null || email.isEmpty()) {
        throw new RuntimeException("Email cannot be empty");
    }

    Users user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    String resetLink = "https://ats-backend-uu58.onrender.com/api/auth/reset-password/" + user.getId();

    String emailTemplate = """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width">
  <title>ATS Circle - Reset Password</title>
</head>
<body style="margin:0;padding:0;background:#f4f6f8;font-family:'Helvetica Neue', Arial, sans-serif;">
  <!-- Outer wrapper -->
  <table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="background:#f4f6f8;padding:40px 0;">
    <tr>
      <td align="center">
        <!-- Card -->
        <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="max-width:600px;width:100%;background:#ffffff;border-radius:12px;box-shadow:0 6px 18px rgba(15,23,42,0.08);overflow:hidden;">
          <!-- Top colored strip -->
          <tr>
            <td style="background:linear-gradient(90deg,#0ea5a0,#2563eb);height:8px;padding:0;"></td>
          </tr>
 
          <!-- Header / Logo -->
          <tr>
            <td style="padding:28px 36px 8px 36px;text-align:left;">
              <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                <tr>
                  <td style="vertical-align:middle;">
                    <table role="presentation" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
                      <tr>
                        <td style="width:48px;height:48px;border-radius:50%;background:#2563eb;text-align:center;vertical-align:middle;">
                          <span style="display:inline-block;font-size:16px;font-weight:700;color:#ffffff;line-height:48px;">ATS</span>
                        </td>
                        <td style="padding-left:10px;vertical-align:middle;">
                          <span style="font-size:20px;font-weight:700;color:#0f172a;">CIRCLE</span>
                        </td>
                      </tr>
                    </table>
                  </td>
                  <td style="text-align:right;vertical-align:middle;font-size:13px;color:#94a3b8;">
                    <span style="display:inline-block;padding:6px 10px;border-radius:999px;background:#eef2ff;color:#3730a3;font-weight:600;">Reset Password</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

          <!-- Reset Password Content -->
          <tr>
            <td style="padding:30px 24px;color:#0f172a;">
              <h2 style="margin:0;font-size:20px;line-height:26px;font-weight:600;">Reset your password</h2>
              <p style="margin:16px 0 0 0;font-size:14px;line-height:20px;color:#475569;">
                Hello <strong>{{first_name}}</strong>, we received a request to reset your account password. Click the button below to create a new one.
              </p>
              
              <!-- CTA Button -->
              <div style="text-align:center;margin:28px 0;">
                <a href="{{reset_link}}" 
                   style="display:inline-block;padding:12px 22px;background:#2563eb;color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;border-radius:6px;">
                  Reset Password
                </a>
              </div>

              <p style="margin:0;font-size:13px;line-height:20px;color:#64748b;">
                If you didn’t request this, you can safely ignore this email. This link will expire in <strong>24 hours</strong>.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="padding:20px 24px;font-size:12px;color:#94a3b8;text-align:center;background:#f9fafb;">
              <p style="margin:0;">Need help? Contact us at 
                <a href="mailto:support@atscircle.com" style="color:#2563eb;text-decoration:none;">support@atscircle.com</a>
              </p>
              <p style="margin:6px 0 0 0;">&copy; 2025 ATS Circle. All rights reserved.</p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""";

    // ✅ Replace placeholders safely
    String htmlContent = emailTemplate
            .replace("{{first_name}}", user.getFirstName())
            .replace("{{reset_link}}", resetLink);

    // Send email
    emailService.sendHtmlEmail(
            user.getEmail(),
            "Password Reset Request - ATS Circle",
            htmlContent
    );
}


    // ✅ Update Password
    public void updatePassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ✅ Get Profile
    public Users getProfile(String email) {
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
