 package com.example.ATSCIRCLE.Controller;

import com.example.ATSCIRCLE.Models.Users;

public class EmailValidator {

    public static boolean isValidEmail(String email, Users.OrganizationType organizationType) {
        if (email == null || email.isEmpty()) return false;

        // ✅ Freelancers can use Gmail, Yahoo, Outlook etc.
        if (organizationType == Users.OrganizationType.FREELANCER) {
            return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
        }

        // ✅ Corporate & Staffing Agency must use work email (no Gmail/Yahoo/etc.)
        if (organizationType == Users.OrganizationType.CORPORATE ||
                organizationType == Users.OrganizationType.STAFFING_AGENCY) {

            // ❌ Block free domains
            String[] freeDomains = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "rediffmail.com"};

            String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

            for (String freeDomain : freeDomains) {
                if (domain.equals(freeDomain)) {
                    return false;
                }
            }

            return email.matches("^[A-Za-z0-9+_.-]+@(.+)$"); // ✅ must be valid format
        }

        return false;
    }
}
 
