package com.example.demo.model;

public class Student {
    private Integer id;
    private String fullname;
    private String role;

    // Геттеры и сеттеры (Инкапсуляция)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}