package com.alumni.search;

import com.alumni.model.Alumni;

import java.util.ArrayList;
import java.util.List;

public class SearchAlgorithms {

    public static int manualCompare(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            double da = ((Number) a).doubleValue();
            double db = ((Number) b).doubleValue();
            return Double.compare(da, db);
        }

        String strA = (a != null) ? a.toString().trim().toLowerCase() : "";
        String strB = (b != null) ? b.toString().trim().toLowerCase() : "";
        return strA.compareTo(strB);
    }

    public static Object extractValue(Alumni alumni, String key) {
        if (alumni == null || key == null) return null;
        switch (key) {
            case "fullName": return alumni.getFullName();
            case "company": return alumni.getCompany();
            case "designation": return alumni.getDesignation();
            case "department": return alumni.getDepartment();
            case "graduationYear": return alumni.getGraduationYear();
            case "experienceYears": return alumni.getExperienceYears();
            case "skills": return alumni.getSkills();
            case "industry": return alumni.getIndustry();
            default: return alumni.getFullName();
        }
    }

    public static List<Alumni> manualSort(List<Alumni> items, String key, boolean ascending) {
        List<Alumni> arr = new ArrayList<>(items);
        if (arr.size() <= 1) return arr;
        quickSort(arr, 0, arr.size() - 1, key, ascending);
        return arr;
    }

    private static void quickSort(List<Alumni> arr, int left, int right, String key, boolean ascending) {
        if (left >= right) return;

        Alumni pivot = arr.get(left + (right - left) / 2);
        Object pivotVal = extractValue(pivot, key);

        int i = left;
        int j = right;

        while (i <= j) {
            if (ascending) {
                while (manualCompare(extractValue(arr.get(i), key), pivotVal) < 0) i++;
                while (manualCompare(extractValue(arr.get(j), key), pivotVal) > 0) j--;
            } else {
                while (manualCompare(extractValue(arr.get(i), key), pivotVal) > 0) i++;
                while (manualCompare(extractValue(arr.get(j), key), pivotVal) < 0) j--;
            }

            if (i <= j) {
                Alumni temp = arr.get(i);
                arr.set(i, arr.get(j));
                arr.set(j, temp);
                i++;
                j--;
            }
        }

        if (left < j) quickSort(arr, left, j, key, ascending);
        if (i < right) quickSort(arr, i, right, key, ascending);
    }

    public static boolean isSorted(List<Alumni> items, String key, boolean ascending) {
        for (int i = 0; i < items.size() - 1; i++) {
            int cmp = manualCompare(extractValue(items.get(i), key), extractValue(items.get(i + 1), key));
            if (ascending && cmp > 0) return false;
            if (!ascending && cmp < 0) return false;
        }
        return true;
    }

    public static SearchResult manualLinearSearch(List<Alumni> items, String query, String[] fields) {
        long startTime = System.nanoTime();
        long comparisons = 0;
        List<Alumni> results = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            double timeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new SearchResult(results, 0, timeMs, "linear", "multi-field");
        }

        String rawQuery = (query != null) ? query.trim().toLowerCase() : "";
        if (rawQuery.isEmpty()) {
            double timeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new SearchResult(new ArrayList<>(items), 0, timeMs, "linear", "multi-field");
        }

        if (fields == null || fields.length == 0) {
            fields = new String[] { "fullName", "company", "designation", "skills", "department", "industry" };
        }

        String[] tokens = rawQuery.split("\\s+");

        for (int i = 0; i < items.size(); i++) {
            Alumni item = items.get(i);
            comparisons++;

            boolean matched = false;
            for (String fieldKey : fields) {
                Object valObj = extractValue(item, fieldKey);
                if (valObj == null) continue;

                String valStr = valObj.toString().toLowerCase();

                if (valStr.contains(rawQuery)) {
                    matched = true;
                    break;
                }

                boolean allTokensFound = true;
                for (String token : tokens) {
                    if (!valStr.contains(token)) {
                        allTokensFound = false;
                        break;
                    }
                }

                if (allTokensFound) {
                    matched = true;
                    break;
                }
            }

            if (matched) {
                results.add(item);
            }
        }

        double timeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return new SearchResult(results, comparisons, timeMs, "linear", "multi-field");
    }

    public static SearchResult manualBinarySearch(List<Alumni> items, String target, String key, boolean exact) {
        long startTime = System.nanoTime();
        long comparisons = 0;
        List<Alumni> results = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            double timeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new SearchResult(results, 0, timeMs, "binary", key);
        }

        String targetStr = (target != null) ? target.trim().toLowerCase() : "";
        if (targetStr.isEmpty()) {
            double timeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            return new SearchResult(new ArrayList<>(items), 0, timeMs, "binary", key);
        }

        if (key == null || key.trim().isEmpty()) {
            key = "fullName";
        }

        // Ensure array is sorted by key
        List<Alumni> sortedItems = items;
        if (!isSorted(items, key, true)) {
            sortedItems = manualSort(items, key, true);
        }

        boolean isNumericKey = "graduationYear".equals(key) || "experienceYears".equals(key);
        boolean isTargetNumeric = false;
        double targetNum = 0.0;
        try {
            targetNum = Double.parseDouble(targetStr);
            isTargetNumeric = true;
        } catch (NumberFormatException ignored) {}

        if (isNumericKey && isTargetNumeric) {
            // Numeric binary search
            int low = 0;
            int high = sortedItems.size() - 1;
            int matchIdx = -1;

            while (low <= high) {
                comparisons++;
                int mid = low + (high - low) / 2;
                Object midObj = extractValue(sortedItems.get(mid), key);
                double midNum = (midObj instanceof Number) ? ((Number) midObj).doubleValue() : 0.0;

                if (midNum == targetNum) {
                    matchIdx = mid;
                    break;
                } else if (midNum < targetNum) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }

            if (matchIdx != -1) {
                // Expand left
                int left = matchIdx;
                while (left > 0) {
                    comparisons++;
                    Object prevObj = extractValue(sortedItems.get(left - 1), key);
                    double prevNum = (prevObj instanceof Number) ? ((Number) prevObj).doubleValue() : 0.0;
                    if (prevNum == targetNum) {
                        left--;
                    } else {
                        break;
                    }
                }

                // Expand right
                int right = matchIdx;
                while (right < sortedItems.size() - 1) {
                    comparisons++;
                    Object nextObj = extractValue(sortedItems.get(right + 1), key);
                    double nextNum = (nextObj instanceof Number) ? ((Number) nextObj).doubleValue() : 0.0;
                    if (nextNum == targetNum) {
                        right++;
                    } else {
                        break;
                    }
                }

                for (int k = left; k <= right; k++) {
                    results.add(sortedItems.get(k));
                }
            }

        } else {
            // String binary search with prefix expansion
            int low = 0;
            int high = sortedItems.size() - 1;
            int matchIdx = -1;

            while (low <= high) {
                comparisons++;
                int mid = low + (high - low) / 2;
                Object midObj = extractValue(sortedItems.get(mid), key);
                String midStr = (midObj != null) ? midObj.toString().trim().toLowerCase() : "";

                int cmp;
                if (exact) {
                    cmp = midStr.compareTo(targetStr);
                } else {
                    if (midStr.startsWith(targetStr)) {
                        cmp = 0;
                    } else {
                        cmp = midStr.compareTo(targetStr);
                    }
                }

                if (cmp == 0) {
                    matchIdx = mid;
                    break;
                } else if (cmp < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }

            if (matchIdx != -1) {
                // Expand left
                int left = matchIdx;
                while (left > 0) {
                    comparisons++;
                    Object prevObj = extractValue(sortedItems.get(left - 1), key);
                    String prevStr = (prevObj != null) ? prevObj.toString().trim().toLowerCase() : "";
                    boolean matches = exact ? prevStr.equals(targetStr) : prevStr.startsWith(targetStr);
                    if (matches) {
                        left--;
                    } else {
                        break;
                    }
                }

                // Expand right
                int right = matchIdx;
                while (right < sortedItems.size() - 1) {
                    comparisons++;
                    Object nextObj = extractValue(sortedItems.get(right + 1), key);
                    String nextStr = (nextObj != null) ? nextObj.toString().trim().toLowerCase() : "";
                    boolean matches = exact ? nextStr.equals(targetStr) : nextStr.startsWith(targetStr);
                    if (matches) {
                        right++;
                    } else {
                        break;
                    }
                }

                for (int k = left; k <= right; k++) {
                    results.add(sortedItems.get(k));
                }
            }
        }

        double timeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return new SearchResult(results, comparisons, timeMs, "binary", key);
    }
}
