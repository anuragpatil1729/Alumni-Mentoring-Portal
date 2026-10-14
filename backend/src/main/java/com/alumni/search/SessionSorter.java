package com.alumni.search;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Team D: Manual Sorting Algorithms for Session History.
 * Implements Insertion Sort, Selection Sort, and Bubble Sort
 * without relying on Java's built-in Collections.sort() or List.sort().
 */
public class SessionSorter {

    public enum Algorithm {
        INSERTION_SORT,
        SELECTION_SORT,
        BUBBLE_SORT
    }

    public static class SortTelemetry {
        private final String algorithm;
        private final String sortKey;
        private final boolean descending;
        private final long comparisons;
        private final long swaps;
        private final long elapsedNanos;
        private final List<JSONObject> sortedList;

        public SortTelemetry(String algorithm, String sortKey, boolean descending,
                             long comparisons, long swaps, long elapsedNanos,
                             List<JSONObject> sortedList) {
            this.algorithm = algorithm;
            this.sortKey = sortKey;
            this.descending = descending;
            this.comparisons = comparisons;
            this.swaps = swaps;
            this.elapsedNanos = elapsedNanos;
            this.sortedList = sortedList;
        }

        public String getAlgorithm() { return algorithm; }
        public String getSortKey() { return sortKey; }
        public boolean isDescending() { return descending; }
        public long getComparisons() { return comparisons; }
        public long getSwaps() { return swaps; }
        public long getElapsedNanos() { return elapsedNanos; }
        public List<JSONObject> getSortedList() { return sortedList; }
    }

    /**
     * Extracts priority weight for sorting.
     */
    public static int getPriorityWeight(String priority) {
        if (priority == null) return 1;
        switch (priority.trim().toUpperCase(Locale.ROOT)) {
            case "URGENT": return 4;
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            case "LOW":
            default: return 1;
        }
    }

    /**
     * Extracts status/severity weight for sorting.
     */
    public static int getSeverityWeight(String status) {
        if (status == null) return 1;
        switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "FAILED": return 4;
            case "PAUSED": return 3;
            case "STARTED": return 2;
            case "COMPLETED":
            default: return 1;
        }
    }

    /**
     * Compare two JSON objects according to the sortKey.
     * Returns:
     *   negative if a < b
     *   zero if a == b
     *   positive if a > b
     */
    public static int compare(JSONObject a, JSONObject b, String sortKey) {
        if (sortKey == null) sortKey = "chronological";
        String key = sortKey.trim().toLowerCase(Locale.ROOT);

        switch (key) {
            case "duration": {
                int d1 = a.optInt("durationMinutes", 0);
                int d2 = b.optInt("durationMinutes", 0);
                return Integer.compare(d1, d2);
            }
            case "priority": {
                int p1 = getPriorityWeight(a.optString("priority", "LOW"));
                int p2 = getPriorityWeight(b.optString("priority", "LOW"));
                return Integer.compare(p1, p2);
            }
            case "severity": {
                int s1 = getSeverityWeight(a.optString("status", "COMPLETED"));
                int s2 = getSeverityWeight(b.optString("status", "COMPLETED"));
                return Integer.compare(s1, s2);
            }
            case "chronological":
            default: {
                String t1 = a.optString("startedAt", a.optString("createdAt", ""));
                String t2 = b.optString("startedAt", b.optString("createdAt", ""));
                return t1.compareTo(t2);
            }
        }
    }

    /**
     * Manual Insertion Sort algorithm.
     * Best case: O(n) for nearly sorted data.
     */
    public static SortTelemetry insertionSort(List<JSONObject> input, String sortKey, boolean descending) {
        List<JSONObject> list = new ArrayList<>(input);
        int n = list.size();
        long comparisons = 0;
        long shifts = 0;
        long start = System.nanoTime();

        for (int i = 1; i < n; i++) {
            JSONObject keyItem = list.get(i);
            int j = i - 1;

            while (j >= 0) {
                comparisons++;
                int cmp = compare(list.get(j), keyItem, sortKey);
                boolean shouldMove = descending ? (cmp < 0) : (cmp > 0);

                if (shouldMove) {
                    list.set(j + 1, list.get(j));
                    shifts++;
                    j--;
                } else {
                    break;
                }
            }
            list.set(j + 1, keyItem);
        }

        long elapsed = System.nanoTime() - start;
        return new SortTelemetry("Insertion Sort", sortKey, descending, comparisons, shifts, elapsed, list);
    }

    /**
     * Manual Selection Sort algorithm.
     * Guaranteed O(n^2) comparisons, minimal swaps (O(n)).
     */
    public static SortTelemetry selectionSort(List<JSONObject> input, String sortKey, boolean descending) {
        List<JSONObject> list = new ArrayList<>(input);
        int n = list.size();
        long comparisons = 0;
        long swaps = 0;
        long start = System.nanoTime();

        for (int i = 0; i < n - 1; i++) {
            int targetIdx = i;
            for (int j = i + 1; j < n; j++) {
                comparisons++;
                int cmp = compare(list.get(j), list.get(targetIdx), sortKey);
                boolean isBetter = descending ? (cmp > 0) : (cmp < 0);
                if (isBetter) {
                    targetIdx = j;
                }
            }

            if (targetIdx != i) {
                JSONObject temp = list.get(i);
                list.set(i, list.get(targetIdx));
                list.set(targetIdx, temp);
                swaps++;
            }
        }

        long elapsed = System.nanoTime() - start;
        return new SortTelemetry("Selection Sort", sortKey, descending, comparisons, swaps, elapsed, list);
    }

    /**
     * Manual Bubble Sort algorithm with early exit optimization.
     */
    public static SortTelemetry bubbleSort(List<JSONObject> input, String sortKey, boolean descending) {
        List<JSONObject> list = new ArrayList<>(input);
        int n = list.size();
        long comparisons = 0;
        long swaps = 0;
        long start = System.nanoTime();

        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                comparisons++;
                int cmp = compare(list.get(j), list.get(j + 1), sortKey);
                boolean shouldSwap = descending ? (cmp < 0) : (cmp > 0);

                if (shouldSwap) {
                    JSONObject temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                    swaps++;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }

        long elapsed = System.nanoTime() - start;
        return new SortTelemetry("Bubble Sort", sortKey, descending, comparisons, swaps, elapsed, list);
    }

    /**
     * Default sort delegator choosing the optimal manual algorithm.
     */
    public static SortTelemetry sort(List<JSONObject> input, String sortKey, boolean descending, Algorithm algorithm) {
        if (algorithm == null) algorithm = Algorithm.INSERTION_SORT;
        switch (algorithm) {
            case BUBBLE_SORT:
                return bubbleSort(input, sortKey, descending);
            case SELECTION_SORT:
                return selectionSort(input, sortKey, descending);
            case INSERTION_SORT:
            default:
                return insertionSort(input, sortKey, descending);
        }
    }
}
