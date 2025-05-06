package com.kienlongbank.classroomservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_classroom", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "classroom_id"})
})
public class StudentClassroom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Student ID is required")
    @Column(name = "student_id")
    private Long studentId;
    
    @NotNull(message = "Classroom ID is required")
    @Column(name = "classroom_id")
    private Long classroomId;
    
    private Double grade;
    
    private String feedback;
    
    private LocalDateTime enrolledAt = LocalDateTime.now();
} 