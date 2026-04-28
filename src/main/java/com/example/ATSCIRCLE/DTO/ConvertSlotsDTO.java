package com.example.ATSCIRCLE.DTO;

import java.util.List;

/**
 * DTO for converting slots to user's timezone
 */
public class ConvertSlotsDTO {
    private List<String> slots;            // Original slots in client timezone
    private String fromTimeZone;           // Source timezone (client timezone)
    private String toTimeZone;            // Target timezone (user timezone)
    
    // Constructor
    public ConvertSlotsDTO() {}

    // Getters and Setters
    public List<String> getSlots() {
        return slots;
    }

    public void setSlots(List<String> slots) {
        this.slots = slots;
    }

    public String getFromTimeZone() {
        return fromTimeZone;
    }

    public void setFromTimeZone(String fromTimeZone) {
        this.fromTimeZone = fromTimeZone;
    }

    public String getToTimeZone() {
        return toTimeZone;
    }

    public void setToTimeZone(String toTimeZone) {
        this.toTimeZone = toTimeZone;
    }
}
