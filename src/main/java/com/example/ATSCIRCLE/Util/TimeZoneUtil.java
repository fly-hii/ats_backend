package com.example.ATSCIRCLE.Util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TimeZoneUtil {

    private static final List<DateTimeFormatter> SUPPORTED_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    private static LocalDateTime parseSlot(String slot) {
        for (DateTimeFormatter fmt : SUPPORTED_FORMATTERS) {
            try {
                return LocalDateTime.parse(slot, fmt);
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("Cannot parse slot: " + slot);
    }

    // Output always "yyyy-MM-dd HH:mm:ss" format
    private static final DateTimeFormatter OUTPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static List<String> convertSlotsToTimeZone(List<String> slots,
                                                       String fromTimeZone,
                                                       String toTimeZone) {
        List<String> convertedSlots = new ArrayList<>();
        if (slots == null || slots.isEmpty()) return convertedSlots;

        try {
            ZoneId sourceZone = ZoneId.of(fromTimeZone);
            ZoneId targetZone = ZoneId.of(toTimeZone);

            for (String slot : slots) {
                String converted = convertSlotToTimeZone(slot, sourceZone, targetZone);
                convertedSlots.add(converted);
            }
        } catch (Exception e) {
            System.err.println("Error converting timezone: " + e.getMessage());
            return slots;
        }
        return convertedSlots;
    }

    public static String convertSlotToTimeZone(String slot,
                                                ZoneId sourceZone,
                                                ZoneId targetZone) {
        try {
            LocalDateTime localDateTime = parseSlot(slot);
            ZonedDateTime sourceZdt = localDateTime.atZone(sourceZone);
            ZonedDateTime targetZdt = sourceZdt.withZoneSameInstant(targetZone);
            return targetZdt.format(OUTPUT_FORMATTER);
        } catch (Exception e) {
            System.err.println("Error converting slot: " + e.getMessage());
            return slot;
        }
    }

    public static boolean isValidTimeZone(String timeZone) {
        try {
            ZoneId.of(timeZone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static LocalDateTime getCurrentTimeInTimeZone(String timeZone) {
        try {
            return LocalDateTime.now(ZoneId.of(timeZone));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    public static String formatWithTimeZone(LocalDateTime localDateTime, String timeZone) {
        try {
            ZoneId zoneId = ZoneId.of(timeZone);
            ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
            return zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz"));
        } catch (Exception e) {
            return localDateTime.format(OUTPUT_FORMATTER);
        }
    }
}