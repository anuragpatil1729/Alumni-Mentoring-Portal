package com.alumni;

import com.alumni.model.Alumni;
import com.alumni.search.SearchAlgorithms;
import com.alumni.search.SearchResult;
import com.alumni.server.HttpServerApp;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchAlgorithmsTest {

    private static List<Alumni> testDataset;
    private static HttpServerApp server;
    private static int httpPort;

    @BeforeAll
    public static void setup() throws Exception {
        testDataset = new ArrayList<>();
        testDataset.add(createMentor(1L, "Aarav Mehta", "Meta", "Computer Engineering", "React, TypeScript", 2020, 5));
        testDataset.add(createMentor(2L, "Aditya Kulkarni", "Nvidia", "Electronics", "CUDA, C++, Python", 2020, 6));
        testDataset.add(createMentor(3L, "Anurag Patil", "Microsoft", "Computer Engineering", "Java, Python, Azure", 2022, 4));
        testDataset.add(createMentor(4L, "Kunal Verma", "Apple", "Computer Engineering", "Swift, iOS", 2019, 7));
        testDataset.add(createMentor(5L, "Neha Nair", "McKinsey", "Mechanical", "Strategy, Consulting", 2018, 7));
        testDataset.add(createMentor(6L, "Priya Sharma", "Amazon", "Information Technology", "Java, AWS", 2021, 5));
        testDataset.add(createMentor(7L, "Rohan Mali", "Google", "Computer Engineering", "AI, Go, Linux", 2022, 3));
        testDataset.add(createMentor(8L, "Sneha Deshmukh", "Goldman Sachs", "Computer Engineering", "Python, SQL", 2023, 2));
        testDataset.add(createMentor(9L, "Tanvi Joshi", "Deloitte", "Information Technology", "Security, Cloud", 2021, 4));
        testDataset.add(createMentor(10L, "Vishwesh Bhilare", "Microsoft", "Computer Engineering", "Java, Python, Cloud", 2022, 4));

        httpPort = 5057;
        server = new HttpServerApp(httpPort);
        server.start();
        Thread.sleep(100);
    }

    @AfterAll
    public static void teardown() {
        if (server != null) server.stop();
    }

    private static Alumni createMentor(Long id, String name, String comp, String dept, String skills, int gradYr, int exp) {
        Alumni a = new Alumni();
        a.setUserId(id);
        a.setFullName(name);
        a.setCompany(comp);
        a.setDepartment(dept);
        a.setSkills(skills);
        a.setGraduationYear(gradYr);
        a.setExperienceYears(exp);
        return a;
    }

    @Test
    @Order(1)
    public void testManualCompare() {
        Assertions.assertEquals(-1, SearchAlgorithms.manualCompare(1, 2));
        Assertions.assertEquals(0, SearchAlgorithms.manualCompare(5, 5));
        Assertions.assertEquals(1, SearchAlgorithms.manualCompare(10, 2));
        Assertions.assertTrue(SearchAlgorithms.manualCompare("Apple", "Google") < 0);
        Assertions.assertEquals(0, SearchAlgorithms.manualCompare("google", "Google"));
        Assertions.assertTrue(SearchAlgorithms.manualCompare("Nvidia", "Amazon") > 0);
    }

    @Test
    @Order(2)
    public void testManualSortStringKey() {
        List<Alumni> sorted = SearchAlgorithms.manualSort(testDataset, "company", true);
        Assertions.assertTrue(SearchAlgorithms.isSorted(sorted, "company", true));
        Assertions.assertEquals("Amazon", sorted.get(0).getCompany());
        Assertions.assertEquals("Nvidia", sorted.get(sorted.size() - 1).getCompany());
    }

    @Test
    @Order(3)
    public void testManualSortNumericalKey() {
        List<Alumni> sorted = SearchAlgorithms.manualSort(testDataset, "experienceYears", true);
        Assertions.assertTrue(SearchAlgorithms.isSorted(sorted, "experienceYears", true));
        Assertions.assertEquals(2, sorted.get(0).getExperienceYears());
        Assertions.assertEquals(7, sorted.get(sorted.size() - 1).getExperienceYears());
    }

    @Test
    @Order(4)
    public void testLinearSearchCompanyMatch() {
        SearchResult res = SearchAlgorithms.manualLinearSearch(testDataset, "microsoft", null);
        Assertions.assertEquals(2, res.getResults().size());
        Assertions.assertEquals(testDataset.size(), res.getComparisons());
        List<String> names = res.getResults().stream().map(Alumni::getFullName).toList();
        Assertions.assertTrue(names.contains("Anurag Patil"));
        Assertions.assertTrue(names.contains("Vishwesh Bhilare"));
    }

    @Test
    @Order(5)
    public void testLinearSearchSkillsMatch() {
        SearchResult res = SearchAlgorithms.manualLinearSearch(testDataset, "CUDA", null);
        Assertions.assertEquals(1, res.getResults().size());
        Assertions.assertEquals("Aditya Kulkarni", res.getResults().get(0).getFullName());
    }

    @Test
    @Order(6)
    public void testLinearSearchMultiToken() {
        SearchResult res = SearchAlgorithms.manualLinearSearch(testDataset, "Python Cloud", null);
        Assertions.assertTrue(res.getResults().size() >= 1);
        Assertions.assertTrue(res.getResults().stream().anyMatch(m -> "Vishwesh Bhilare".equals(m.getFullName())));
    }

    @Test
    @Order(7)
    public void testLinearSearchNoMatch() {
        SearchResult res = SearchAlgorithms.manualLinearSearch(testDataset, "NonExistentXYZCompany", null);
        Assertions.assertEquals(0, res.getResults().size());
        Assertions.assertEquals(testDataset.size(), res.getComparisons());
    }

    @Test
    @Order(8)
    public void testLinearSearchEmptyQuery() {
        SearchResult res = SearchAlgorithms.manualLinearSearch(testDataset, "", null);
        Assertions.assertEquals(testDataset.size(), res.getResults().size());
        Assertions.assertEquals(0, res.getComparisons());
    }

    @Test
    @Order(9)
    public void testBinarySearchExactMatch() {
        List<Alumni> sorted = SearchAlgorithms.manualSort(testDataset, "fullName", true);
        SearchResult res = SearchAlgorithms.manualBinarySearch(sorted, "Rohan Mali", "fullName", true);

        Assertions.assertEquals(1, res.getResults().size());
        Assertions.assertEquals("Rohan Mali", res.getResults().get(0).getFullName());
        // For N=10, binary comparisons <= 8
        Assertions.assertTrue(res.getComparisons() <= 8, "Expected <= 8 comparisons, got: " + res.getComparisons());
    }

    @Test
    @Order(10)
    public void testBinarySearchPrefixMatches() {
        SearchResult res = SearchAlgorithms.manualBinarySearch(testDataset, "Micro", "company", false);
        Assertions.assertEquals(2, res.getResults().size());
        for (Alumni a : res.getResults()) {
            Assertions.assertEquals("Microsoft", a.getCompany());
        }
    }

    @Test
    @Order(11)
    public void testBinarySearchNumericalGraduationYear() {
        SearchResult res = SearchAlgorithms.manualBinarySearch(testDataset, "2022", "graduationYear", false);
        // Anurag, Rohan, Vishwesh
        Assertions.assertEquals(3, res.getResults().size());
        for (Alumni a : res.getResults()) {
            Assertions.assertEquals(2022, a.getGraduationYear());
        }
    }

    @Test
    @Order(12)
    public void testBinarySearchNotFound() {
        SearchResult res = SearchAlgorithms.manualBinarySearch(testDataset, "Tesla", "company", true);
        Assertions.assertEquals(0, res.getResults().size());
        Assertions.assertTrue(res.getComparisons() <= 5);
    }

    @Test
    @Order(13)
    public void testBinarySearchEmptyDataset() {
        SearchResult res = SearchAlgorithms.manualBinarySearch(new ArrayList<>(), "Query", "fullName", false);
        Assertions.assertEquals(0, res.getResults().size());
        Assertions.assertEquals(0, res.getComparisons());
    }

    @Test
    @Order(14)
    public void testLinearAndBinaryEquivalenceSingleItem() {
        String query = "Google";
        SearchResult lin = SearchAlgorithms.manualLinearSearch(testDataset, query, new String[] { "company" });
        SearchResult bin = SearchAlgorithms.manualBinarySearch(testDataset, query, "company", true);

        Assertions.assertEquals(lin.getResults().size(), bin.getResults().size());
        Assertions.assertEquals(lin.getResults().get(0).getUserId(), bin.getResults().get(0).getUserId());
        Assertions.assertEquals(lin.getResults().get(0).getFullName(), bin.getResults().get(0).getFullName());
    }

    @Test
    @Order(15)
    public void testLinearAndBinaryEquivalenceMultipleItems() {
        String query = "Microsoft";
        SearchResult lin = SearchAlgorithms.manualLinearSearch(testDataset, query, new String[] { "company" });
        SearchResult bin = SearchAlgorithms.manualBinarySearch(testDataset, query, "company", true);

        Assertions.assertEquals(2, lin.getResults().size());
        Assertions.assertEquals(2, bin.getResults().size());

        List<Long> linIds = lin.getResults().stream().map(Alumni::getUserId).sorted().toList();
        List<Long> binIds = bin.getResults().stream().map(Alumni::getUserId).sorted().toList();
        Assertions.assertEquals(linIds, binIds);
    }

    @Test
    @Order(16)
    public void testHttpSearchLinear() throws Exception {
        JSONObject res = sendGet("/api/mentors/search?query=Microsoft&algorithm=linear");
        Assertions.assertTrue(res.getBoolean("success"));
        Assertions.assertEquals("linear", res.getString("algorithm"));
        Assertions.assertTrue(res.getJSONObject("metrics").getInt("totalRecords") > 0);
        Assertions.assertTrue(res.getJSONObject("metrics").getLong("comparisons") > 0);
        Assertions.assertTrue(res.getJSONArray("data").length() >= 2);
    }

    @Test
    @Order(17)
    public void testHttpSearchBinaryLogarithmic() throws Exception {
        JSONObject res = sendGet("/api/mentors/search?query=Google&algorithm=binary&key=company");
        Assertions.assertTrue(res.getBoolean("success"));
        Assertions.assertEquals("binary", res.getString("algorithm"));
        Assertions.assertEquals("company", res.getString("searchKey"));
        Assertions.assertTrue(res.getJSONArray("data").length() >= 1);
    }

    @Test
    @Order(18)
    public void testHttpSearchFacetFilters() throws Exception {
        JSONObject res = sendGet("/api/mentors/search?query=Microsoft&department=Computer&minExperience=3");
        Assertions.assertTrue(res.getBoolean("success"));
        JSONArray data = res.getJSONArray("data");
        Assertions.assertTrue(data.length() > 0);
        for (int i = 0; i < data.length(); i++) {
            JSONObject m = data.getJSONObject(i);
            Assertions.assertTrue(m.getString("department").toLowerCase().contains("computer"));
            Assertions.assertTrue(m.getInt("experienceYears") >= 3);
        }
    }

    @Test
    @Order(19)
    public void testHttpGetMentorById() throws Exception {
        JSONObject allRes = sendGet("/api/mentors");
        Assertions.assertTrue(allRes.getBoolean("success"));
        JSONArray data = allRes.getJSONArray("data");
        long firstId = data.getJSONObject(0).getLong("id");

        JSONObject singleRes = sendGet("/api/mentors/" + firstId);
        Assertions.assertTrue(singleRes.getBoolean("success"));
        Assertions.assertEquals(firstId, singleRes.getJSONObject("data").getLong("id"));

        // 404 test
        URL url = URI.create("http://127.0.0.1:" + httpPort + "/api/mentors/999999").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        Assertions.assertEquals(404, conn.getResponseCode());
    }

    private JSONObject sendGet(String path) throws Exception {
        URL url = URI.create("http://127.0.0.1:" + httpPort + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString());
        }
    }
}
