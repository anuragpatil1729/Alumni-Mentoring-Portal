package com.alumni;

import com.alumni.service.ConflictDetector;
import com.alumni.service.ConflictDetector.ConflictCheckResult;
import com.alumni.service.ConflictDetector.TimeSlot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Team 2 & Team 3: Automated Unit Tests for Session Conflict Detection Logic.
 */
public class ConflictDetectionTest {

    private final LocalDate today = LocalDate.of(2026, 10, 15);
    private final LocalDate tomorrow = LocalDate.of(2026, 10, 16);

    @Test
    @DisplayName("Exact Match Conflict: Identical start and end times on same date")
    public void testExactMatchConflict() {
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);

        boolean overlap = ConflictDetector.hasIntervalOverlap(today, start, end, today, start, end);
        Assertions.assertTrue(overlap, "Identical intervals on the same date must conflict");
    }

    @Test
    @DisplayName("Partial Overlap Conflict: Candidate starts before and ends inside existing slot")
    public void testPartialOverlapStartsBefore() {
        LocalTime s1 = LocalTime.of(9, 30);
        LocalTime e1 = LocalTime.of(10, 30);

        LocalTime s2 = LocalTime.of(10, 0);
        LocalTime e2 = LocalTime.of(11, 0);

        boolean overlap = ConflictDetector.hasIntervalOverlap(today, s1, e1, today, s2, e2);
        Assertions.assertTrue(overlap, "Candidate overlapping the start of an existing slot must conflict");
    }

    @Test
    @DisplayName("Partial Overlap Conflict: Candidate starts inside and ends after existing slot")
    public void testPartialOverlapStartsInside() {
        LocalTime s1 = LocalTime.of(10, 30);
        LocalTime e1 = LocalTime.of(11, 30);

        LocalTime s2 = LocalTime.of(10, 0);
        LocalTime e2 = LocalTime.of(11, 0);

        boolean overlap = ConflictDetector.hasIntervalOverlap(today, s1, e1, today, s2, e2);
        Assertions.assertTrue(overlap, "Candidate overlapping the end of an existing slot must conflict");
    }

    @Test
    @DisplayName("Enclosing Conflict: Candidate completely surrounds existing slot")
    public void testEnclosingConflict() {
        LocalTime s1 = LocalTime.of(9, 0);
        LocalTime e1 = LocalTime.of(12, 0);

        LocalTime s2 = LocalTime.of(10, 0);
        LocalTime e2 = LocalTime.of(11, 0);

        boolean overlap = ConflictDetector.hasIntervalOverlap(today, s1, e1, today, s2, e2);
        Assertions.assertTrue(overlap, "Candidate that encloses existing slot must conflict");
    }

    @Test
    @DisplayName("Enclosed Conflict: Candidate is completely inside existing slot")
    public void testEnclosedInsideConflict() {
        LocalTime s1 = LocalTime.of(10, 15);
        LocalTime e1 = LocalTime.of(10, 45);

        LocalTime s2 = LocalTime.of(10, 0);
        LocalTime e2 = LocalTime.of(11, 0);

        boolean overlap = ConflictDetector.hasIntervalOverlap(today, s1, e1, today, s2, e2);
        Assertions.assertTrue(overlap, "Candidate enclosed inside existing slot must conflict");
    }

    @Test
    @DisplayName("Adjacent Boundary Test: Candidate ends exactly when existing slot starts (No Conflict)")
    public void testAdjacentBoundaryBeforeNoConflict() {
        LocalTime s1 = LocalTime.of(9, 0);
        LocalTime e1 = LocalTime.of(10, 0);

        LocalTime s2 = LocalTime.of(10, 0);
        LocalTime e2 = LocalTime.of(11, 0);

        boolean overlap = ConflictDetector.hasIntervalOverlap(today, s1, e1, today, s2, e2);
        Assertions.assertFalse(overlap, "Adjacent slots touching at boundary (10:00) must NOT conflict");
    }

    @Test
    @DisplayName("Adjacent Boundary Test: Candidate starts exactly when existing slot ends (No Conflict)")
    public void testAdjacentBoundaryAfterNoConflict() {
        LocalTime s1 = LocalTime.of(11, 0);
        LocalTime e1 = LocalTime.of(12, 0);

        LocalTime s2 = LocalTime.of(10, 0);
        LocalTime e2 = LocalTime.of(11, 0);

        boolean overlap = ConflictDetector.hasIntervalOverlap(today, s1, e1, today, s2, e2);
        Assertions.assertFalse(overlap, "Adjacent slots touching at boundary (11:00) must NOT conflict");
    }

    @Test
    @DisplayName("Date Isolation Test: Same time range on different days (No Conflict)")
    public void testDifferentDateNoConflict() {
        LocalTime s1 = LocalTime.of(10, 0);
        LocalTime e1 = LocalTime.of(11, 0);

        boolean overlap = ConflictDetector.hasIntervalOverlap(today, s1, e1, tomorrow, s1, e1);
        Assertions.assertFalse(overlap, "Slots on different dates must NOT conflict");
    }

    @Test
    @DisplayName("Multi-Slot Candidate Verification with Mixed Slots")
    public void testMultiSlotEvaluation() {
        List<TimeSlot> existing = new ArrayList<>();
        existing.add(new TimeSlot(1, 101, today, LocalTime.of(9, 0), LocalTime.of(10, 0), "Morning Checkin"));
        existing.add(new TimeSlot(2, 101, today, LocalTime.of(14, 0), LocalTime.of(15, 0), "System Design"));
        existing.add(new TimeSlot(3, 101, tomorrow, LocalTime.of(11, 0), LocalTime.of(12, 0), "Resume Review"));

        // 1. Valid non-conflicting slot between 10:00 and 11:00
        ConflictCheckResult r1 = ConflictDetector.checkCandidateConflict(
                today, LocalTime.of(10, 0), LocalTime.of(11, 0), existing);
        Assertions.assertFalse(r1.hasConflict());

        // 2. Conflicting slot overlapping slot #2 (14:30 to 15:30)
        ConflictCheckResult r2 = ConflictDetector.checkCandidateConflict(
                today, LocalTime.of(14, 30), LocalTime.of(15, 30), existing);
        Assertions.assertTrue(r2.hasConflict());
        Assertions.assertEquals(2, r2.getConflictingSlot().getId());
        Assertions.assertTrue(r2.getMessage().contains("Schedule conflict detected"));

        // 3. Invalid end time before start time
        ConflictCheckResult r3 = ConflictDetector.checkCandidateConflict(
                today, LocalTime.of(16, 0), LocalTime.of(15, 0), existing);
        Assertions.assertTrue(r3.hasConflict());
        Assertions.assertTrue(r3.getMessage().contains("strictly after start time"));
    }
}
