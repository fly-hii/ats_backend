package com.example.ATSCIRCLE.DTO;

public class ScheduleMeetDTO {

    private String candidateApplicationId;
    private String selectedSlot;       // One of the convertedSlots from GET /converted-slots
    private String userTimeZone;       // Same timezone user selected from dropdown
    private String jobId;
    private String candidateEmail;
    private String candidateName;
    private String toEmail;            // Candidate email — TO field
    private String bccEmail;           // Client email   — real BCC field

    public ScheduleMeetDTO() {}

    public String getCandidateApplicationId() { return candidateApplicationId; }
    public void setCandidateApplicationId(String v) { this.candidateApplicationId = v; }

    public String getSelectedSlot() { return selectedSlot; }
    public void setSelectedSlot(String v) { this.selectedSlot = v; }

    public String getUserTimeZone() { return userTimeZone; }
    public void setUserTimeZone(String v) { this.userTimeZone = v; }

    public String getJobId() { return jobId; }
    public void setJobId(String v) { this.jobId = v; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String v) { this.candidateEmail = v; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String v) { this.candidateName = v; }

    public String getToEmail() { return toEmail; }
    public void setToEmail(String v) { this.toEmail = v; }

    public String getBccEmail() { return bccEmail; }
    public void setBccEmail(String v) { this.bccEmail = v; }
}