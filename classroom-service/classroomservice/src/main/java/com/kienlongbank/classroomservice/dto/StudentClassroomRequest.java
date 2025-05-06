package com.kienlongbank.classroomservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentClassroomRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "Classroom ID is required")
    private Long classroomId;
} 