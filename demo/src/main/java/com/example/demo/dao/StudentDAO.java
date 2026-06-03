package com.example.demo.dao;

import com.example.demo.model.Student;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class StudentDAO {
    private final DataSource dataSource;

    public StudentDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Авторизация
    public Student authenticate(String login, String password) throws SQLException {
        String sql = "SELECT id, fullname, role FROM students WHERE login = ? AND password = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student();
                    student.setId(rs.getInt("id"));
                    student.setFullname(rs.getString("fullname"));
                    student.setRole(rs.getString("role"));
                    return student;
                }
            }
        }
        return null; // Пользователь не найден
    }
}