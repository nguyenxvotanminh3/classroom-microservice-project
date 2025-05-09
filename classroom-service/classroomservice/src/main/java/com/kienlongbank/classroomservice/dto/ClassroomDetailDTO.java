package com.kienlongbank.classroomservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomDetailDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long teacherId;
    private int capacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 