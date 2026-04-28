package com.example.ATSCIRCLE.Models;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "users")
public class Users {

    @Id
    private String id;

    @Field("first_name")
    private String firstName;

    @Field("last_name")
    private String lastName;

    private String password;

    @Field("contact_number")
    private String contactNumber;

    @Field("country_code")
    private String countryCode;

    @Indexed(unique = true)
    private String email;

    @Field("organization_type")
    private OrganizationType organizationType;   
    @Field("organization_name")
    private String organizationName;
private String GSTnumber;
private String accountno;
private String ifsccode;
private String pannumber;
private String accountname;
private String bankname;

private String streetLine1;
private String streetLine2;
private String city;
private String state;
private String country;
private String zipcode;





    private Role role = Role.ADMIN;

    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    private ActionStatus actionStatus = ActionStatus.PENDING;

    @Field("no_of_users")
    private Integer noOfUsers = 0;   

    private LocalDateTime createdAt = LocalDateTime.now();

    // ---------- ENUMS ----------

    public enum Role {
        SUPERADMIN,
        ADMIN
    }

    public enum PaymentStatus {
        PAID,
        UNPAID,
        FREETRIAL
    }

    public enum ActionStatus {
        PENDING,
        ACTIVATED,
        DEACTIVATED,
        ON_HOLD,
        SUSPENDED,
        BANNED
    }

    public enum OrganizationType {
        FREELANCER,        // "Freelance Recruiter"
        STAFFING_AGENCY,   // "Staffing Agency"
        CORPORATE          // "Corporate"
    }

    // ---------- CONSTRUCTORS ----------

    public Users() {}

    public Users(String firstName, String lastName, String password, String contactNumber,
                 String countryCode, String email, OrganizationType organizationType, String organizationName,
                 Role role, PaymentStatus paymentStatus, ActionStatus actionStatus, Integer noOfUsers) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.contactNumber = contactNumber;
        this.countryCode = countryCode;
        this.email = email;
        this.organizationType = organizationType;
        this.organizationName = organizationName;
        this.role = role;
        this.paymentStatus = paymentStatus;
        this.actionStatus = actionStatus;
        this.noOfUsers = noOfUsers;
        this.createdAt = LocalDateTime.now();
    }

    // ---------- GETTERS & SETTERS ----------

    public String getId() { return id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public OrganizationType getOrganizationType() { return organizationType; }
    public void setOrganizationType(OrganizationType organizationType) { this.organizationType = organizationType; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public ActionStatus getActionStatus() { return actionStatus; }
    public void setActionStatus(ActionStatus actionStatus) { this.actionStatus = actionStatus; }

    public String getaccountname() { return accountname; }
    public void setaccountname(String accountname) { this.accountname = accountname; }
  public String getbankname() { return bankname; }
    public void setbankname(String bankname) { this.bankname = bankname; }


    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

public String getGSTnumber() {
    return GSTnumber;
}

public void setGSTnumber(String GSTnumber) {
    this.GSTnumber = GSTnumber;
}

public String getAccountno() {
    return accountno;
}

public void setAccountno(String accountno) {
    this.accountno = accountno;
}

public String getIfsccode() {
    return ifsccode;
}

public void setIfsccode(String ifsccode) {
    this.ifsccode = ifsccode;
}

public String getPannumber() {
    return pannumber;
}

public void setPannumber(String pannumber) {
    this.pannumber = pannumber;
}

public String getStreetLine1() {
    return streetLine1;
}
   public Integer getNoOfUsers() { return noOfUsers; }
    public void setNoOfUsers(Integer noOfUsers) { this.noOfUsers = noOfUsers; }

public void setStreetLine1(String streetLine1) {
    this.streetLine1 = streetLine1;
}

public String getStreetLine2() {
    return streetLine2;
}

public void setStreetLine2(String streetLine2) {
    this.streetLine2 = streetLine2;
}

public String getCity() {
    return city;
}

public void setCity(String city) {
    this.city = city;
}

public String getState() {
    return state;
}

public void setState(String state) {
    this.state = state;
}

public String getCountry() {
    return country;
}

public void setCountry(String country) {
    this.country = country;
}

public String getZipcode() {
    return zipcode;
}

public void setZipcode(String zipcode) {
    this.zipcode = zipcode;
}

private String googleAccessToken;
private String googleRefreshToken;
private LocalDateTime googleTokenExpiry;
private String googleEmail;
private boolean googleConnected = false;

// Getters & Setters
public String getGoogleAccessToken() { return googleAccessToken; }
public void setGoogleAccessToken(String googleAccessToken) { this.googleAccessToken = googleAccessToken; }

public String getGoogleRefreshToken() { return googleRefreshToken; }
public void setGoogleRefreshToken(String googleRefreshToken) { this.googleRefreshToken = googleRefreshToken; }

public LocalDateTime getGoogleTokenExpiry() { return googleTokenExpiry; }
public void setGoogleTokenExpiry(LocalDateTime googleTokenExpiry) { this.googleTokenExpiry = googleTokenExpiry; }

public String getGoogleEmail() { return googleEmail; }
public void setGoogleEmail(String googleEmail) { this.googleEmail = googleEmail; }

public boolean isGoogleConnected() { return googleConnected; }
public void setGoogleConnected(boolean googleConnected) { this.googleConnected = googleConnected; }


// ===== MICROSOFT (OUTLOOK) =====
private String microsoftAccessToken;
private String microsoftRefreshToken;
private LocalDateTime microsoftTokenExpiry;
private String microsoftEmail;
private boolean microsoftConnected = false;

// ===== DEFAULT EMAIL PROVIDER =====
public enum EmailProvider {
    NONE,
    GOOGLE,
    MICROSOFT
}

private EmailProvider defaultEmailProvider = EmailProvider.NONE;
public String getMicrosoftAccessToken() { return microsoftAccessToken; }
public void setMicrosoftAccessToken(String t) { this.microsoftAccessToken = t; }

public String getMicrosoftRefreshToken() { return microsoftRefreshToken; }
public void setMicrosoftRefreshToken(String t) { this.microsoftRefreshToken = t; }

public LocalDateTime getMicrosoftTokenExpiry() { return microsoftTokenExpiry; }
public void setMicrosoftTokenExpiry(LocalDateTime t) { this.microsoftTokenExpiry = t; }

public String getMicrosoftEmail() { return microsoftEmail; }
public void setMicrosoftEmail(String e) { this.microsoftEmail = e; }

public boolean isMicrosoftConnected() { return microsoftConnected; }
public void setMicrosoftConnected(boolean b) { this.microsoftConnected = b; }

public EmailProvider getDefaultEmailProvider() { return defaultEmailProvider; }
public void setDefaultEmailProvider(EmailProvider p) { this.defaultEmailProvider = p; }
}
