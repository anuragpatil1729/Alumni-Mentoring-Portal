package com.alumni;

import com.alumni.search.SessionSorter;
import com.alumni.search.SessionSorter.Algorithm;
import com.alumni.search.SessionSorter.SortTelemetry;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Team A: Verification of Manual Sorting Algorithm Accuracy (Bubble, Selection, Insertion).
 */
public class SessionHistorySortTest {

    private List<JSONObject> sampleSessions;

    @BeforeEach
    public void setup() {
        sampleSessions = new ArrayList<>();

        sampleSessions.add(new JSONObject()
                .put("id", 1)
                .put("durationMinutes", 30)
                .put("priority", "LOW")
                .put("status", "COMPLETED")
                .put("startedAt", "2026-10-10 10:00:00"));

        sampleSessions.add(new JSONObject()
                .put("id", 2)
                .put("durationMinutes", 90)
                .put("priority", "URGENT")
                .put("status", "FAILED")
                .put("startedAt", "2026-10-12 14:00:00"));

        sampleSessions.add(new JSONObject()
                .put("id", 3)
                .put("durationMinutes", 45)
                .put("priority", "HIGH")
                .put("status", "PAUSED")
                .put("startedAt", "2026-10-11 09:00:00"));

        sampleSessions.add(new JSONObject()
                .put("id", 4)
                .put("durationMinutes", 60)
                .put("priority", "MEDIUM")
                .put("status", "STARTED")
                .put("startedAt", "2026-10-13 16:00:00"));
    }

    @Test
    @DisplayName("Insertion Sort: Duration Descending (90 > 60 > 45 > 30)")
    public void testInsertionSortDurationDesc() {
        SortTelemetry telemetry = SessionSorter.insertionSort(sampleSessions, "duration", true);
        List<JSONObject> sorted = telemetry.getSortedList();

        Assertions.assertEquals(4, sorted.size());
        Assertions.assertEquals(90, sorted.get(0).getInt("durationMinutes"));
        Assertions.assertEquals(60, sorted.get(1).getInt("durationMinutes"));
        Assertions.assertEquals(45, sorted.get(2).getInt("durationMinutes"));
        Assertions.assertEquals(30, sorted.get(3).getInt("durationMinutes"));

        Assertions.assertTrue(telemetry.getComparisons() > 0);
        Assertions.assertTrue(telemetry.getElapsedNanos() > 0);
    }

    @Test
    @DisplayName("Insertion Sort: Priority Descending (URGENT > HIGH > MEDIUM > LOW)")
    public void testInsertionSortPriorityDesc() {
        SortTelemetry telemetry = SessionSorter.insertionSort(sampleSessions, "priority", true);
        List<JSONObject> sorted = telemetry.getSortedList();

        Assertions.assertEquals("URGENT", sorted.get(0).getString("priority"));
        Assertions.assertEquals("HIGH", sorted.get(1).getString("priority"));
        Assertions.assertEquals("MEDIUM", sorted.get(2).getString("priority"));
        Assertions.assertEquals("LOW", sorted.get(3).getString("priority"));
    }

    @Test
    @DisplayName("Selection Sort: Severity Descending (FAILED > PAUSED > STARTED > COMPLETED)")
    public void testSelectionSortSeverityDesc() {
        SortTelemetry telemetry = SessionSorter.selectionSort(sampleSessions, "severity", true);
        List<JSONObject> sorted = telemetry.getSortedList();

        Assertions.assertEquals("FAILED", sorted.get(0).getString("status"));
        Assertions.assertEquals("PAUSED", sorted.get(1).getString("status"));
        Assertions.assertEquals("STARTED", sorted.get(2).getString("status"));
        Assertions.assertEquals("COMPLETED", sorted.get(3).getString("status"));
    }

    @Test
    @DisplayName("Bubble Sort: Chronological Ascending Order")
    public void testBubbleSortChronologicalAsc() {
        SortTelemetry telemetry = SessionSorter.bubbleSort(sampleSessions, "chronological", false);
        List<JSONObject> sorted = telemetry.getSortedList();

        Assertions.assertEquals(1, sorted.get(0).getInt("id")); // 2026-10-10
        Assertions.assertEquals(3, sorted.get(1).getInt("id")); // 2026-10-11
        Assertions.assertEquals(2, sorted.get(2).getInt("id")); // 2026-10-12
        Assertions.assertEquals(4, sorted.get(3).getInt("id")); // 2026-10-13
    }

    @Test
    @DisplayName("Equivalence Check: All 3 Algorithms Yield Identical Sorting Output")
    public void testAllAlgorithmsProduceIdenticalOutput() {
        SortTelemetry ins = SessionSorter.sort(sampleSessions, "priority", true, Algorithm.INSERTION_SORT);
        SortTelemetry sel = SessionSorter.sort(sampleSessions, "priority", true, Algorithm.SELECTION_SORT);
        SortTelemetry bub = SessionSorter.sort(sampleSessions, "priority", true, Algorithm.BUBBLE_SORT);

        for (int i = 0; i < sampleSessions.size(); i++) {
            int idIns = ins.getSortedList().get(i).getInt("id");
            int idSel = sel.getSortedList().get(i).getInt("id");
            int idBub = bub.getSortedList().get(i).getInt("id");

            Assertions.assertEquals(idIns, idSel, "Insertion Sort and Selection Sort must match at index " + i);
            Assertions.assertEquals(idIns, idBub, "Insertion Sort and Bubble Sort must match at index " + i);
        }
    }

    @Test
    @DisplayName("Edge Cases: Empty List and Single Item List Handled Safely")
    public void testEdgeCases() {
        // 1. Empty List
        List<JSONObject> empty = new ArrayList<>();
        SortTelemetry rEmpty = SessionSorter.insertionSort(empty, "duration", true);
        Assertions.assertTrue(rEmpty.getSortedList().isEmpty());
        Assertions.assertEquals(0, rEmpty.getComparisons());

        // 2. Single Item List
        List<JSONObject> single = new ArrayList<>();
        single.add(new JSONObject().put("id", 10).put("durationMinutes", 45));
        SortTelemetry rSingle = SessionSorter.insertionSort(single, "duration", true);
        Assertions.assertEquals(1, rSingle.getSortedList().size());
        Assertions.assertEquals(10, rSingle.getSortedList().get(0).getInt("id"));
    }

    @Test
    @DisplayName("Duplicates & Stability: Duplicate Duration Maintained")
    public void testDuplicateKeysHandling() {
        List<JSONObject> duplicates = new ArrayList<>();
        duplicates.add(new JSONObject().put("id", 1).put("durationMinutes", 30));
        duplicates.add(new JSONObject().put("id", 2).put("durationMinutes", 30));
        duplicates.add(new JSONObject().put("id", 3).put("durationMinutes", 30));

        SortTelemetry res = SessionSorter.insertionSort(duplicates, "duration", true);
        Assertions.assertEquals(3, res.getSortedList().size());
        // Insertion sort is stable
        Assertions.assertEquals(1, res.getSortedList().get(0).getInt("id"));
        Assertions.assertEquals(2, res.getSortedList().get(1).getInt("id"));
        Assertions.assertEquals(3, res.getSortedList().get(2).getInt("id"));
    }
}
