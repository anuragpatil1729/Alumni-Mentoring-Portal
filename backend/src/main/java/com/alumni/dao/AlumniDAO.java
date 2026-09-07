package com.alumni.dao;

import com.alumni.config.DBConnection;
import com.alumni.model.Alumni;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AlumniDAO
 *
 * Provides JDBC data access methods to query alumni mentor profiles from local MySQL.
 */
public class AlumniDAO {

    private static final String SELECT_ALUMNI_BASE =
            "SELECT u.id AS user_id, u.full_name, u.email, u.mobile_number, " +
            "       a.department, a.graduation_year, a.company, a.designation, " +
            "       a.linkedin_profile, a.experience_years, a.industry, a.skills, " +
            "       a.bio, a.max_mentees " +
            "  FROM users u " +
            "  JOIN alumni a ON u.id = a.user_id ";

    /**
     * Retrieves all alumni mentor records from local MySQL.
     */
    public List<Alumni> getAllAlumni() throws SQLException {
        List<Alumni> list = new ArrayList<>();
        String sql = SELECT_ALUMNI_BASE + " ORDER BY u.id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToAlumni(rs));
            }
        }
        return list;
    }

    /**
     * Finds alumni by company name.
     */
    public List<Alumni> findByCompany(String company) throws SQLException {
        List<Alumni> list = new ArrayList<>();
        String sql = SELECT_ALUMNI_BASE + " WHERE LOWER(a.company) LIKE ? ORDER BY u.id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + company.toLowerCase().trim() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAlumni(rs));
                }
            }
        }
        return list;
    }

    /**
     * Finds alumni mentor by user ID.
     */
    public Alumni findById(long userId) throws SQLException {
        String sql = SELECT_ALUMNI_BASE + " WHERE u.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAlumni(rs);
                }
            }
        }
        return null;
    }

    /**
     * Counts total alumni in the database.
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(1) FROM alumni";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Alumni mapResultSetToAlumni(ResultSet rs) throws SQLException {
        Alumni alumni = new Alumni();
        alumni.setUserId(rs.getLong("user_id"));
        alumni.setFullName(rs.getString("full_name"));
        alumni.setEmail(rs.getString("email"));
        alumni.setMobileNumber(rs.getString("mobile_number"));
        alumni.setDepartment(rs.getString("department"));
        alumni.setGraduationYear(rs.getInt("graduation_year"));
        alumni.setCompany(rs.getString("company"));
        alumni.setDesignation(rs.getString("designation"));
        alumni.setLinkedInProfile(rs.getString("linkedin_profile"));

        int exp = rs.getInt("experience_years");
        alumni.setExperienceYears(rs.wasNull() ? null : exp);

        alumni.setIndustry(rs.getString("industry"));
        alumni.setSkills(rs.getString("skills"));
        alumni.setBio(rs.getString("bio"));

        int max = rs.getInt("max_mentees");
        alumni.setMaxMentees(rs.wasNull() ? null : max);

        return alumni;
    }
}
