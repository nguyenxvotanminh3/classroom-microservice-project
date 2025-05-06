package com.kienlongbank.temporal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassroomDto {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long teacherId;
    private String teacherName;
    private Integer capacity;
    private int[] createdAt;
    private int[] updatedAt;
} 