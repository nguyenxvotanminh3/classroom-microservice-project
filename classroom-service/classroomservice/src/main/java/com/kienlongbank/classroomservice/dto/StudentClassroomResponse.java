package com.kienlongbank.classroomservice.dto;


import com.kienlongbank.classroomservice.client.UserServiceClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentClassroomResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long studentId;
    private UserServiceClient.UserDto studentDetails;
    private Long classroomId;
    private ClassroomResponse classroom;
    private Double grade;
    private String feedback;
    private LocalDateTime enrolledAt;
} 