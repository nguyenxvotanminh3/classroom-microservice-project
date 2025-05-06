package com.kienlongbank.classroomservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long teacherId;
    private String teacherName;
    private int capacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor without teacherName for backward compatibility
    public ClassroomResponse(Long id, String name, String code, String description, 
                           Long teacherId, int capacity, 
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.teacherId = teacherId;
        this.capacity = capacity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.teacherName = "Unknown";
    }
} 