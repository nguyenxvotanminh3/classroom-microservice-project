package com.kienlongbank.classroomservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Classroom name is required")
    @Size(max=255)
    private String name;
    
    @NotBlank(message = "Classroom code is required")
    @Length(min = 5)
    private String code;
    
    private String description;
    
    @NotNull(message = "Teacher ID is required")
    @Min(1)
    private Long teacherId;

    @Min(1)
    private int capacity = 30;
} 