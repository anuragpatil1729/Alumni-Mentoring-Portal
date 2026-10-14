package com.alumni.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Team 3: Manual Session Conflict Detection Engine.
 * Evaluates temporal overlap between requested mentoring sessions / slot bookings
 * using pure mathematical interval comparison:
 *   Overlap exists iff: max(start1, start2) < min(end1, end2) on the same date.
 */
public class ConflictDetector {

    public static class TimeSlot {
        private final long id;
        private final long ownerId; // mentorId or alumniId
        private final LocalDate date;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final String label;

        public TimeSlot(long id, long ownerId, LocalDate date, LocalTime startTime, LocalTime endTime, String label) {
            this.id = id;
            this.ownerId = ownerId;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.label = label;
        }

        public long getId() { return id; }
        public long getOwnerId() { return ownerId; }
        public LocalDate getDate() { return date; }
        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }
        public String getLabel() { return label; }
    }

    public static class ConflictCheckResult {
        private final boolean conflict;
        private final String message;
        private final TimeSlot conflictingSlot;

        public ConflictCheckResult(boolean conflict, String message, TimeSlot conflictingSlot) {
            this.conflict = conflict;
            this.message = message;
            this.conflictingSlot = conflictingSlot;
        }

        public boolean hasConflict() { return conflict; }
        public String getMessage() { return message; }
        public TimeSlot getConflictingSlot() { return conflictingSlot; }
    }

    /**
     * Mathematical interval overlap formula:
     * Overlap iff dates match AND max(start1, start2) < min(end1, end2).
     */
    public static boolean hasIntervalOverlap(LocalDate d1, LocalTime s1, LocalTime e1,
                                            LocalDate d2, LocalTime s2, LocalTime e2) {
        if (d1 == null || d2 == null || s1 == null || e1 == null || s2 == null || e2 == null) {
            return false;
        }

        if (!d1.equals(d2)) {
            return false; // Different days cannot conflict
        }

        LocalTime maxStart = s1.isAfter(s2) ? s1 : s2;
        LocalTime minEnd = e1.isBefore(e2) ? e1 : e2;

        return maxStart.isBefore(minEnd);
    }

    /**
     * Checks if a candidate time slot conflicts with any existing booked slots.
     */
    public static ConflictCheckResult checkCandidateConflict(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            List<TimeSlot> existingSlots) {

        if (date == null || startTime == null || endTime == null) {
            return new ConflictCheckResult(true, "Date, start time, and end time are required.", null);
        }

        if (!endTime.isAfter(startTime)) {
            return new ConflictCheckResult(true, "Session end time must be strictly after start time.", null);
        }

        if (existingSlots == null || existingSlots.isEmpty()) {
            return new ConflictCheckResult(false, "Slot is available.", null);
        }

        for (TimeSlot existing : existingSlots) {
            if (hasIntervalOverlap(date, startTime, endTime, existing.getDate(), existing.getStartTime(), existing.getEndTime())) {
                String msg = String.format(
                        "Schedule conflict detected: Overlaps with existing session [%s] scheduled from %s to %s.",
                        existing.getLabel() != null ? existing.getLabel() : "Slot #" + existing.getId(),
                        existing.getStartTime(),
                        existing.getEndTime()
                );
                return new ConflictCheckResult(true, msg, existing);
            }
        }

        return new ConflictCheckResult(false, "No conflicts detected. Slot is available.", null);
    }
}
