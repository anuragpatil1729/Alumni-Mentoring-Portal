package com.alumni.search;

import com.alumni.model.Alumni;

import java.util.List;

public class SearchResult {
    private final List<Alumni> results;
    private final long comparisons;
    private final double executionTimeMs;
    private final String algorithm;
    private final String searchKey;

    public SearchResult(List<Alumni> results, long comparisons, double executionTimeMs, String algorithm, String searchKey) {
        this.results = results;
        this.comparisons = comparisons;
        this.executionTimeMs = executionTimeMs;
        this.algorithm = algorithm;
        this.searchKey = searchKey;
    }

    public List<Alumni> getResults() {
        return results;
    }

    public long getComparisons() {
        return comparisons;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getSearchKey() {
        return searchKey;
    }
}
